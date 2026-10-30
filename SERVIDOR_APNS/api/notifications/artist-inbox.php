<?php
declare(strict_types=1);
require __DIR__ . '/../_bootstrap.php';
$user = current_user(true);
$uid = (int)$user['id']; $pdo = db();
if ($_SERVER['REQUEST_METHOD']==='GET') {
    $s=$pdo->prepare("SELECT q.id,p.artist_name,e.title,e.town,q.delivered_at AS created_at,q.read_at
      FROM ios_artist_alert_pending q
      JOIN ios_artist_bells p ON p.user_id=q.user_id AND p.artist_key=q.artist_key
      JOIN events e ON e.id=q.event_id
      WHERE q.user_id=? AND q.delivered_at IS NOT NULL
      ORDER BY q.delivered_at DESC,q.id DESC LIMIT 100");
    $s->execute([$uid]);
    $items=[];
    foreach ($s->fetchAll() as $r) {
        $items[]=['id'=>(int)$r['id'],'artist_name'=>$r['artist_name'],
            'title'=>'Nova activitat de '.$r['artist_name'],
            'body'=>$r['title'].' · '.$r['town'],
            'read_at'=>$r['read_at'],'created_at'=>$r['created_at']];
    }
    send_json(['ok'=>true,'notifications'=>$items]);
}
if ($_SERVER['REQUEST_METHOD']!=='POST') fail('Mètode no permés.',405,'METHOD_NOT_ALLOWED');
$id=(int)(request_data()['id']??0);
if ($id<=0) fail('Identificador no vàlid.',422,'VALIDATION');
$pdo->prepare('UPDATE ios_artist_alert_pending SET read_at=COALESCE(read_at,UTC_TIMESTAMP()) WHERE id=? AND user_id=? AND delivered_at IS NOT NULL')
    ->execute([$id,$uid]);
send_json(['ok'=>true]);
