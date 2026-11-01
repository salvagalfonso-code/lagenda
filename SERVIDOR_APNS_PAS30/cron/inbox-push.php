<?php
/** Deliver real inbox notifications to iOS APNs, never by the mobile application.
 * CLI only, environment-gated. No Apple keys, device tokens or credentials are logged.
 */
declare(strict_types=1);
if (PHP_SAPI !== 'cli') { http_response_code(404); exit(1); }
require dirname(__DIR__) . '/api/_bootstrap.php';
require __DIR__ . '/_ios_push_prefs.php';
function envRequired(string $name): string {
    $value = trim((string)getenv($name));
    if ($value === '') throw new RuntimeException("Missing env: $name");
    return $value;
}
function base64url(string $value): string { return rtrim(strtr(base64_encode($value), '+/', '-_'), '='); }
function ecdsaDerToJose(string $der): string {
    $i=0; $len=strlen($der);
    if ($len<8 || ord($der[$i++]) !== 0x30) throw new RuntimeException('Invalid ECDSA DER');
    $total=ord($der[$i++]);
    if ($total & 0x80) {
        $parts=$total & 0x7f; if ($parts<1 || $parts>2) throw new RuntimeException('Invalid DER length');
        $total=0; for ($j=0;$j<$parts;$j++) $total=($total<<8)|ord($der[$i++]);
    }
    $pieces=[];
    foreach ([0,1] as $_) {
        if ($i+2>$len || ord($der[$i++])!==0x02) throw new RuntimeException('Invalid DER integer');
        $n=ord($der[$i++]);
        if ($i+$n>$len) throw new RuntimeException('Invalid DER integer size');
        $raw=ltrim(substr($der,$i,$n),"\x00"); $i+=$n;
        if (strlen($raw)>32) throw new RuntimeException('ECDSA coordinate too large');
        $pieces[]=str_pad($raw,32,"\x00",STR_PAD_LEFT);
    }
    return implode('', $pieces);
}
function apnsJwt(string $keyId, string $teamId, string $p8): string {
    $key=openssl_pkey_get_private('file://' . $p8);
    if ($key===false) throw new RuntimeException('APNs signing key could not be opened');
    $a=base64url(json_encode(['alg'=>'ES256','kid'=>$keyId],JSON_THROW_ON_ERROR));
    $b=base64url(json_encode(['iss'=>$teamId,'iat'=>time()],JSON_THROW_ON_ERROR));
    if (!openssl_sign("$a.$b",$der,$key,OPENSSL_ALGO_SHA256)) throw new RuntimeException('Failed to sign APNs token');
    return "$a.$b." . base64url(ecdsaDerToJose($der));
}
function sendAPNs(string $deviceToken, string $environment, string $topic, string $jwt, array $notification): array {
    $host=$environment==='production' ? 'api.push.apple.com' : 'api.sandbox.push.apple.com';
    $curl=curl_init("https://$host/3/device/$deviceToken");
    $body=json_encode($notification,JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES|JSON_THROW_ON_ERROR);
    curl_setopt_array($curl,[
        CURLOPT_POST=>true,CURLOPT_HTTP_VERSION=>CURL_HTTP_VERSION_2_0,
        CURLOPT_POSTFIELDS=>$body,CURLOPT_RETURNTRANSFER=>true,
        CURLOPT_CONNECTTIMEOUT=>6,CURLOPT_TIMEOUT=>15,
        CURLOPT_HTTPHEADER=>[
            'authorization: bearer '.$jwt,'apns-topic: '.$topic,'apns-push-type: alert',
            'apns-priority: 10','content-type: application/json'
        ],
    ]);
    $response=curl_exec($curl);
    $code=(int)curl_getinfo($curl,CURLINFO_HTTP_CODE);
    $reason=is_string($response) ? (json_decode($response,true)['reason'] ?? '') : '';
    $error=curl_error($curl); curl_close($curl);
    return [$code,(string)$reason,$error];
}

$pdo=db();
$lock='agenda_apns_inbox_delivery_v1';
try {
    if (getenv('APNS_SEND_ENABLED')!=='1') throw new RuntimeException('APNs sending disabled');
    $team=envRequired('APNS_TEAM_ID');$keyId=envRequired('APNS_KEY_ID');
    $p8=envRequired('APNS_P8_PATH');$topic=envRequired('APNS_TOPIC');
    if (!is_readable($p8)) throw new RuntimeException('APNs .p8 key unavailable');
    if (!function_exists('curl_init') || !defined('CURL_HTTP_VERSION_2_0')) throw new RuntimeException('HTTP2 required');
    $cacheFile=(string)(getenv('APNS_JWT_CACHE_PATH')?:dirname($p8).'/apns-jwt-cache.json');
    $cache=is_file($cacheFile)?json_decode((string)@file_get_contents($cacheFile),true):null;
    if (is_array($cache)&&isset($cache['created'],$cache['jwt'])&&time()-(int)$cache['created']<1500 && time()-(int)$cache['created']>=0) $jwt=(string)$cache['jwt'];
    else {$jwt=apnsJwt($keyId,$team,$p8);@file_put_contents($cacheFile,json_encode(['created'=>time(),'jwt'=>$jwt]),LOCK_EX);@chmod($cacheFile,0600);}
    $stmt=$pdo->prepare('SELECT GET_LOCK(?,0)');$stmt->execute([$lock]);
    if ((int)$stmt->fetchColumn()!==1) exit(0);
    $rows=$pdo->query("SELECT n.id,n.user_id,n.title,n.body,n.deep_link_type,n.deep_link_value,
          d.device_token,d.environment,r.attempts,r.retry_after,
          c.target_json
       FROM admin_notification_inbox n
       JOIN users u ON u.id=n.user_id AND u.active=1
       JOIN ios_apns_devices d ON d.user_id=n.user_id AND d.enabled=1
       JOIN agenda_user_client_state prefs ON prefs.user_id=n.user_id
         AND prefs.state_key='ios_push_enabled' AND JSON_UNQUOTE(JSON_EXTRACT(prefs.value_json,'$.v'))='true'
       LEFT JOIN admin_notification_campaigns c ON c.id=n.campaign_id
       LEFT JOIN ios_apns_inbox_delivery r ON r.inbox_id=n.id AND r.device_token=d.device_token
       WHERE n.created_at>DATE_SUB(UTC_TIMESTAMP(), INTERVAL 7 DAY)
         AND r.accepted_at IS NULL AND (r.attempts IS NULL OR r.attempts<8)
         AND (r.retry_after IS NULL OR r.retry_after<=UTC_TIMESTAMP())
       ORDER BY n.created_at,n.id LIMIT 200")->fetchAll();
    $receipt=$pdo->prepare("INSERT INTO ios_apns_inbox_delivery(inbox_id,device_token,attempts,accepted_at,retry_after)
       VALUES(?,?,?, ?,?) ON DUPLICATE KEY UPDATE attempts=VALUES(attempts),accepted_at=VALUES(accepted_at),retry_after=VALUES(retry_after)");
    $drop=$pdo->prepare('DELETE FROM ios_apns_devices WHERE device_token=?');
    $prefs=[];$accepted=0;$failed=0;$deferred=0;
    foreach($rows as $row){
        $uid=(int)$row['user_id'];
        if (!isset($prefs[$uid])) $prefs[$uid]=ios_apns_preferences($pdo,$uid);
        $kind=strtoupper((string)($row['deep_link_type']??''));
        $meta=json_decode((string)($row['target_json']??''),true);
        $isCustom=false;
        if (is_array($meta) && (int)($meta['reminder_user_id']??0)===$uid && (int)($meta['event_id']??0)>0) {
            $q=$pdo->prepare('SELECT custom_time FROM agenda_user_reminders WHERE user_id=? AND event_id=?');
            $q->execute([$uid,(int)$meta['event_id']]);$isCustom=(int)$q->fetchColumn()===1;
        }
        if (!$isCustom && !ios_apns_allowed($prefs[$uid],$kind)) continue;
        if ($isCustom && ($prefs[$uid]['ios_push_enabled']??false)!==true) continue;
        $wait=ios_apns_quiet_end($prefs[$uid],$isCustom);
        $attempts=(int)($row['attempts']??0);
        if ($wait!==null) {
            $receipt->execute([(int)$row['id'],$row['device_token'],$attempts,null,$wait->format('Y-m-d H:i:s')]);
            $deferred++;continue;
        }
        $payload=['aps'=>['alert'=>['title'=>mb_substr((string)$row['title'],0,160),
                    'body'=>mb_substr((string)$row['body'],0,200)],'sound'=>'default'],
                  'type'=>$kind ?: 'GENERAL'];
        $id=filter_var($row['deep_link_value'],FILTER_VALIDATE_INT);
        if (in_array($kind,['EVENT','EVENT_ID','EVENT_DETAIL'],true)&&$id!==false&&$id>0)
            $payload['event_id']=(int)$id;
        if (in_array($kind,['ARTIST','NEW_ARTIST_EVENT'],true))
            $payload['artist_name']=mb_substr((string)$row['deep_link_value'],0,190);
        [$code,$reason,$transportError]=sendAPNs($row['device_token'],$row['environment'],$topic,$jwt,$payload);
        if ($code===200) {
            $receipt->execute([(int)$row['id'],$row['device_token'],$attempts+1,gmdate('Y-m-d H:i:s'),null]);$accepted++;
        } else {
            $receipt->execute([(int)$row['id'],$row['device_token'],$attempts+1,null,gmdate('Y-m-d H:i:s',time()+900)]);
            if ($code===410 || in_array($reason,['BadDeviceToken','Unregistered','DeviceTokenNotForTopic'],true))
                $drop->execute([$row['device_token']]);
            $failed++;
        }
    }
    echo "APNs inbox accepted=$accepted failed=$failed deferred=$deferred\n";
} catch(Throwable $e) {
    error_log('APNs inbox worker: '.$e->getMessage());exit(1);
} finally {
    try{$q=$pdo->prepare('SELECT RELEASE_LOCK(?)');$q->execute([$lock]);}catch(Throwable $_){}
}
