<?php
declare(strict_types=1);
require_once __DIR__.'/../_bootstrap.php';
require_once __DIR__.'/_engine.php';
if(($_SERVER['REQUEST_METHOD']??'GET')!=='POST')fail('Mètode no permés.',405,'METHOD_NOT_ALLOWED');
$raw=request_data();
if(!is_array($raw)||!is_array($raw['preferences']??null))fail('Falten preferències de Per a tu.',422,'VALIDATION');
$providedFollows=$raw['follows']??[];
if(!is_array($providedFollows))fail('Seguiments invàlids.',422,'VALIDATION');
$prefs=fy_preferences($raw['preferences']);
try {
    $pdo=db();
    $user=bearer_token()!==null ? current_user(true) : null;
    $userId=(int)($user['id']??0);
    if($userId>0){
        $stmt=$pdo->prepare('SELECT entity_type,entity_key,entity_label FROM user_follows WHERE user_id=? ORDER BY entity_type,entity_key');
        $stmt->execute([$userId]);
        $follows=$stmt->fetchAll(PDO::FETCH_ASSOC);
        // For registered users, category tastes are authoritative on the server.
        // Do not reintroduce stale local categories from a second device.
        $prefs['categories']=[];
        $prefs['subcategories']=[];
        // A registered account reads the SAME saved feed switches on Android and iOS.
        // Client-submitted switches are not authoritative once account preferences exist.
        $feedPrefQuery=$pdo->prepare('SELECT suggestions,discovery,valencian_first FROM user_for_you_preferences WHERE user_id=? LIMIT 1');
        $feedPrefQuery->execute([$userId]);
        $storedFeedPrefs=$feedPrefQuery->fetch(PDO::FETCH_ASSOC);
        if ($storedFeedPrefs !== false) {
            $prefs['suggestions']=(bool)$storedFeedPrefs['suggestions'];
            $prefs['discovery']=(string)$storedFeedPrefs['discovery'];
            $prefs['valencian_first']=(bool)$storedFeedPrefs['valencian_first'];
        }
        $query=$pdo->prepare("SELECT entity_type,entity_key,interaction_count FROM user_affinities WHERE user_id=? AND last_seen_at>=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 15 DAY) AND (permanent=1 OR expires_at IS NULL OR expires_at>UTC_TIMESTAMP()) ORDER BY entity_type,entity_key");
        $query->execute([$userId]);
        $affinities=$query->fetchAll(PDO::FETCH_ASSOC);
    } else {
        $follows=array_slice($providedFollows,0,200);
        $affinities=[];
    }
    $tz=new DateTimeZone('Europe/Madrid');
    $start=(new DateTimeImmutable('now',$tz))->format('Y-m-d');
    $until=(new DateTimeImmutable('now',$tz))->modify('+2 months')->format('Y-m-d');
    $revision=$pdo->query("SELECT COALESCE(MAX(updated_at),'') FROM events WHERE status='published'")->fetchColumn();
    $revision=(string)($revision?:'');
    // Server-issued cache key: never trust a signature supplied by either mobile client.
    $signature=hash('sha256',json_encode([$userId,$start,$until,$revision,$prefs,$follows,$affinities,'v1'],JSON_UNESCAPED_UNICODE));
    if($userId>0 && !($raw['refresh']??false)) {
        try {
            $q=$pdo->prepare('SELECT ids_json FROM for_you_server_cache WHERE user_id=? AND signature=? AND expires_at>UTC_TIMESTAMP() LIMIT 1');
            $q->execute([$userId,$signature]);
            $cached=$q->fetchColumn();
            if($cached!==false){
                $ids=json_decode((string)$cached,true);
                if(is_array($ids))send_json(['ok'=>true,'ids'=>array_values(array_map('intval',$ids)),'calculated_on_server'=>true,'cache'=>'hit','signature'=>$signature,'source'=>'server-v1']);
            }
        }catch(Throwable $ignored) { /* Migration may not be deployed yet. */ }
    }
    $q=$pdo->prepare("SELECT * FROM events WHERE status='published' AND COALESCE(end_date,event_date)>=? AND event_date<=? ORDER BY event_date,COALESCE(event_time,'23:59:59'),id LIMIT 10001");
    $q->execute([$start,$until]);
    $rows=$q->fetchAll(PDO::FETCH_ASSOC);
    if(count($rows)>10000)fail('Catàleg massa gran per a una selecció segura.',503,'CATALOG_LIMIT');
    $ids=array_map(static fn($r)=>(int)$r['id'],$rows);
    // Same canonical event payload as Android events/index.php; preload relations once.
    preload_event_payload_context($ids);
    $events=[];
    foreach($rows as $r){
        $e=event_payload($r);
        // Exactly the Android AgendaReservationPolicy predicate.
        $e['free_internal_reservation']= !empty($e['ticket_enabled']) &&
            (float)($e['ticket_price']??0)===0.0 && trim((string)($e['ticket_url']??''))==='';
        $events[]=$e;
    }
    $assetBase=__DIR__.'/data/';
    $assets=[];
    foreach(['styles'=>'style_affinity_catalog_v3.json','territory'=>'agenda_territori_v277.json','taxonomy'=>'taxonomia_android.json'] as $k=>$file){
        $assets[$k]=json_decode((string)file_get_contents($assetBase.$file),true,512,JSON_THROW_ON_ERROR);
    }
    $selected=fy_select($events,$follows,$prefs,$assets,$affinities);
    if($userId>0){
        try {
            $q=$pdo->prepare('INSERT INTO for_you_server_cache (user_id,signature,ids_json,expires_at,updated_at) VALUES (?,?,?,DATE_ADD(UTC_TIMESTAMP(),INTERVAL 30 MINUTE),UTC_TIMESTAMP()) ON DUPLICATE KEY UPDATE signature=VALUES(signature),ids_json=VALUES(ids_json),expires_at=VALUES(expires_at),updated_at=UTC_TIMESTAMP()');
            $q->execute([$userId,$signature,json_encode($selected,JSON_THROW_ON_ERROR)]);
        }catch(Throwable $ignored){ /* The response remains valid without persistent cache. */ }
    }
    send_json(['ok'=>true,'ids'=>$selected,'calculated_on_server'=>true,'cache'=>'miss','signature'=>$signature,'source'=>'server-v1']);
}catch(Throwable $e){
    error_log('for-you/feed.php '.$e->getMessage());
    fail('No s’ha pogut calcular el feed al servidor.',503,'FEED_SERVER_ERROR');
}
