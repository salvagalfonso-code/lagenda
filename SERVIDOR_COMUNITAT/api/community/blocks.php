<?php
declare(strict_types=1);
require_once __DIR__ . '/_community.php';
$user=community_user(); $uid=(int)$user['id']; $pdo=community_pdo();
if ($_SERVER['REQUEST_METHOD']==='GET') {
    $q=$pdo->prepare("SELECT b.blocked_user_id user_id,
        COALESCE(NULLIF(up.display_name,''),u.username) display_name
        FROM community_user_blocks b JOIN users u ON u.id=b.blocked_user_id
        LEFT JOIN user_profiles up ON up.user_id=u.id
        WHERE b.blocker_user_id=? ORDER BY b.created_at DESC LIMIT 500");
    $q->execute([$uid]);
    community_json(200,['ok'=>true,'blocked'=>array_map(static fn($r)=>['user_id'=>(int)$r['user_id'],
        'display_name'=>(string)$r['display_name']],$q->fetchAll())]);
}
if ($_SERVER['REQUEST_METHOD']!=='POST') community_json(405,['ok'=>false,'message'=>'Mètode no permés.']);
$in=community_input(); $target=(int)($in['user_id']??0); $action=strtoupper(trim((string)($in['action']??'')));
if ($target<=0 || $target===$uid || !in_array($action,['BLOCK','UNBLOCK'],true)) {
    community_json(422,['ok'=>false,'message'=>'Usuari o acció invàlida.']);
}
if ($action==='BLOCK') {
    $q=$pdo->prepare('SELECT id FROM users WHERE id=? AND active=1 LIMIT 1');
    $q->execute([$target]);
    if (!$q->fetchColumn()) community_json(404,['ok'=>false,'message'=>'Usuari no trobat.']);
    $pdo->prepare('INSERT IGNORE INTO community_user_blocks(blocker_user_id,blocked_user_id) VALUES(?,?)')->execute([$uid,$target]);
} else {
    $pdo->prepare('DELETE FROM community_user_blocks WHERE blocker_user_id=? AND blocked_user_id=?')->execute([$uid,$target]);
}
community_json(200,['ok'=>true,'blocked'=>$action==='BLOCK']);
