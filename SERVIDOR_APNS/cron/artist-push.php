<?php
/**
 * L'AGENDA iOS · push provider. Run only under CLI from cron every 5 minutes.
 * NO API secret, Key ID, Team ID, or .p8 file belongs in the mobile ZIP/application.
 * Non-reentrant; no notifications before credentials and deployment are configured.
 */
declare(strict_types=1);
if (PHP_SAPI !== 'cli') { http_response_code(404); exit(1); }
require dirname(__DIR__) . '/api/_bootstrap.php';
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
$lock='agenda_artist_apns_push_v1';
try {
    if (getenv('APNS_SEND_ENABLED') !== '1') throw new RuntimeException('Set APNS_SEND_ENABLED=1 when ready');
    $team=envRequired('APNS_TEAM_ID'); $keyId=envRequired('APNS_KEY_ID');
    $p8=envRequired('APNS_P8_PATH'); $topic=envRequired('APNS_TOPIC');
    if (!is_file($p8) || !is_readable($p8)) throw new RuntimeException('P8 not readable');
    if (!function_exists('curl_init') || !defined('CURL_HTTP_VERSION_2_0')) throw new RuntimeException('cURL HTTP/2 required');
    // Token signing keys must not be re-signed on every five-minute cron tick.
    $cachePath = (string)(getenv('APNS_JWT_CACHE_PATH') ?: dirname($p8) . '/apns-jwt-cache.json');
    $cache = is_file($cachePath) ? json_decode((string)@file_get_contents($cachePath), true) : null;
    if (is_array($cache) && isset($cache['created'],$cache['jwt']) &&
        (time()-(int)$cache['created']) < 1500 && (time()-(int)$cache['created']) >= 0) {
        $jwt = (string)$cache['jwt'];
    } else {
        $jwt=apnsJwt($keyId,$team,$p8);
        @file_put_contents($cachePath,json_encode(['created'=>time(),'jwt'=>$jwt]),LOCK_EX);
        @chmod($cachePath,0600);
    }
    $s=$pdo->prepare('SELECT GET_LOCK(?,0)'); $s->execute([$lock]);
    if ((int)$s->fetchColumn()!==1) exit(0);
    // Match artist names from event artists and festival sessions (never invent artist associations).
    // Do not notify for an event that already existed before the artist bell was enabled.
    $discover=[
        "INSERT IGNORE INTO ios_artist_alert_pending(user_id,artist_key,event_id,discovered_at)
         SELECT DISTINCT p.user_id,p.artist_key,e.id,UTC_TIMESTAMP()
           FROM ios_artist_bells p JOIN event_artists a
             ON LOWER(TRIM(a.artist_name)) COLLATE utf8mb4_unicode_ci=p.artist_key
           JOIN events e ON e.id=a.event_id
         WHERE p.enabled=1 AND e.status='published' AND e.deleted_at IS NULL
           AND COALESCE(e.end_date,e.event_date) >= UTC_DATE()
           AND COALESCE(e.published_at,e.created_at)>=p.activated_at",
        "INSERT IGNORE INTO ios_artist_alert_pending(user_id,artist_key,event_id,discovered_at)
         SELECT DISTINCT p.user_id,p.artist_key,e.id,UTC_TIMESTAMP()
           FROM ios_artist_bells p JOIN special_event_schedule ss
             ON LOWER(TRIM(COALESCE(NULLIF(ss.artist_name,''),ss.group_name))) COLLATE utf8mb4_unicode_ci=p.artist_key
           JOIN events e ON e.id=ss.event_id
         WHERE p.enabled=1 AND ss.active=1 AND e.status='published' AND e.deleted_at IS NULL
           AND COALESCE(e.end_date,e.event_date)>=UTC_DATE()
           AND GREATEST(COALESCE(e.published_at,e.created_at),ss.created_at)>=p.activated_at"
    ];
    foreach ($discover as $sql) $pdo->exec($sql);
    // A change of preference or cancelled event must not send a stale notification.
    $pdo->exec("DELETE q FROM ios_artist_alert_pending q
        LEFT JOIN ios_artist_bells p ON p.user_id=q.user_id AND p.artist_key=q.artist_key
        LEFT JOIN events e ON e.id=q.event_id
        WHERE q.delivered_at IS NULL AND (p.id IS NULL OR p.enabled=0 OR e.id IS NULL
          OR e.status<>'published' OR e.deleted_at IS NOT NULL OR COALESCE(e.end_date,e.event_date)<UTC_DATE())");
    $q=$pdo->query("SELECT q.id,q.user_id,q.artist_key,q.event_id,q.attempts,q.discovered_at,
            p.artist_name,p.frequency,e.title,e.town,e.event_date
        FROM ios_artist_alert_pending q
        JOIN ios_artist_bells p ON p.user_id=q.user_id AND p.artist_key=q.artist_key AND p.enabled=1
        JOIN events e ON e.id=q.event_id AND e.status='published' AND e.deleted_at IS NULL
        JOIN users u ON u.id=q.user_id AND u.active=1
        WHERE q.delivered_at IS NULL AND q.attempts<8
          AND (q.retry_after IS NULL OR q.retry_after<=UTC_TIMESTAMP())
        ORDER BY q.discovered_at,q.id LIMIT 300");
    $rows=$q->fetchAll();
    $now=new DateTimeImmutable('now',new DateTimeZone('Europe/Madrid'));
    $hour=(int)$now->format('G'); $weekday=(int)$now->format('N');
    $grouped=[];
    foreach ($rows as $row) {
        $f=$row['frequency'];
        if ($f==='DAILY' && $hour!==19) continue;
        if ($f==='WEEKLY' && ($weekday!==1 || $hour!==19)) continue;
        $key=(int)$row['user_id'].'|'.$row['artist_key'];
        $grouped[$key][]=$row;
    }
    $deviceStmt=$pdo->prepare('SELECT device_token,environment FROM ios_apns_devices WHERE user_id=? AND enabled=1 ORDER BY last_seen_at DESC LIMIT 5');
    $done=$pdo->prepare('UPDATE ios_artist_alert_pending SET delivered_at=UTC_TIMESTAMP() WHERE id=? AND delivered_at IS NULL');
    $retry=$pdo->prepare('UPDATE ios_artist_alert_pending SET attempts=attempts+1,retry_after=DATE_ADD(UTC_TIMESTAMP(),INTERVAL 15 MINUTE) WHERE id=? AND delivered_at IS NULL');
    $deleteDevice=$pdo->prepare('DELETE FROM ios_apns_devices WHERE device_token=?');
    $accepted=0; $failed=0;
    foreach ($grouped as $group) {
        $first=$group[0]; $uid=(int)$first['user_id'];
        $deviceStmt->execute([$uid]); $devices=$deviceStmt->fetchAll();
        if (!$devices) continue; // Keep queue until a device is registered.
        $count=count($group);
        if ($count===1) {
            $title='Nova activitat de '.$first['artist_name'];
            $body=mb_substr((string)$first['title'],0,150).' · '.(string)$first['town'];
        } else {
            $title=$count.' activitats noves de '.$first['artist_name'];
            $body='Consulta les noves dates a L’Agenda.';
        }
        $payload=['aps'=>['alert'=>['title'=>$title,'body'=>$body],'sound'=>'default'],
            'type'=>'ARTIST','artist_name'=>$first['artist_name']];
        if ($count===1) $payload['event_id']=(int)$first['event_id'];
        $sent=false; $attempted=false;
        foreach ($devices as $device) {
            $attempted=true;
            [$code,$reason,$transportError]=sendAPNs($device['device_token'],$device['environment'],$topic,$jwt,$payload);
            if ($code===200) { $sent=true; continue; }
            // Apple reports invalid/unregistered tokens; avoid retrying those devices forever.
            if ($code===410 || in_array($reason,['BadDeviceToken','Unregistered','DeviceTokenNotForTopic'],true)) {
                $deleteDevice->execute([$device['device_token']]);
            } else { error_log('Agenda APNs response status='.$code.' reason='.$reason); }
        }
        foreach ($group as $row) {
            if ($sent) $done->execute([$row['id']]);
            elseif ($attempted) $retry->execute([$row['id']]);
        }
        if ($sent) $accepted++; else $failed++;
    }
    echo "Groups accepted=$accepted failed=$failed\n";
} catch (Throwable $e) {
    error_log('Agenda APNs worker: '.$e->getMessage());
    exit(1);
} finally {
    try { $s=$pdo->prepare('SELECT RELEASE_LOCK(?)'); $s->execute([$lock]); } catch (Throwable $_) { }
}
