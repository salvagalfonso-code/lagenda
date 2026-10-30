<?php
declare(strict_types=1);
require_once __DIR__ . '/_community.php';
$user=community_user(); $uid=(int)$user['id']; $pdo=community_pdo();
if ($_SERVER['REQUEST_METHOD']==='GET') {
    $gid=(int)($_GET['group_id']??0); community_require_creator($gid,$uid);
    $q=$pdo->prepare("SELECT m.user_id,COALESCE(NULLIF(up.display_name,''),u.username) display_name,
       m.role,m.status FROM community_members m JOIN users u ON u.id=m.user_id
       LEFT JOIN user_profiles up ON up.user_id=m.user_id WHERE m.group_id=? ORDER BY m.role='CREATOR' DESC,m.requested_at ASC");
    $q->execute([$gid]);
    $q2=$pdo->prepare("SELECT b.user_id,COALESCE(NULLIF(up.display_name,''),u.username) display_name FROM community_group_bans b
       JOIN users u ON u.id=b.user_id LEFT JOIN user_profiles up ON up.user_id=b.user_id WHERE b.group_id=?");
    $q2->execute([$gid]);
    community_json(200,['ok'=>true,'members'=>array_map(static fn($r)=>['user_id'=>(int)$r['user_id'],'display_name'=>$r['display_name'],'role'=>$r['role'],'status'=>$r['status']],$q->fetchAll()),
       'bans'=>array_map(static fn($r)=>['user_id'=>(int)$r['user_id'],'display_name'=>$r['display_name']],$q2->fetchAll())]);
}
if ($_SERVER['REQUEST_METHOD']!=='POST') community_json(405,['ok'=>false,'message'=>'Mètode no permés.']);
$in=community_input(); $gid=(int)($in['group_id']??0);
$action=strtoupper(trim((string)($in['action']??''))); $target=(int)($in['user_id']??0); $mid=(int)($in['message_id']??0);
community_require_creator($gid,$uid);
if (!in_array($action,['DELETE_MESSAGE','BAN_MEMBER','UNBAN_MEMBER'],true)) community_json(422,['ok'=>false,'message'=>'Acció incorrecta.']);
if ($action==='DELETE_MESSAGE') {
    if ($mid<=0) community_json(422,['ok'=>false,'message'=>'Missatge incorrecte.']);
    $q=$pdo->prepare('UPDATE community_messages SET deleted_at=UTC_TIMESTAMP() WHERE id=? AND group_id=? AND deleted_at IS NULL');
    $q->execute([$mid,$gid]);
    if ($q->rowCount()===0) community_json(404,['ok'=>false,'message'=>'Missatge no trobat o ja eliminat.']);
} else {
    if ($target<=0 || $target===$uid) community_json(422,['ok'=>false,'message'=>'No pots aplicar esta acció al creador.']);
    $q=$pdo->prepare('SELECT user_id FROM community_members WHERE group_id=? AND user_id=? AND role=\'MEMBER\'');
    $q->execute([$gid,$target]);
    if (!$q->fetchColumn()) community_json(404,['ok'=>false,'message'=>'La persona no figura en el tauló.']);
    if ($action==='BAN_MEMBER') {
        $pdo->prepare('INSERT INTO community_group_bans(group_id,user_id,banned_by_user_id) VALUES(?,?,?) ON DUPLICATE KEY UPDATE banned_by_user_id=VALUES(banned_by_user_id),created_at=UTC_TIMESTAMP()')->execute([$gid,$target,$uid]);
        $pdo->prepare("UPDATE community_members SET status='REJECTED',updated_at=UTC_TIMESTAMP() WHERE group_id=? AND user_id=? AND role='MEMBER'")->execute([$gid,$target]);
        community_refresh_full_status($gid);
    } else {
        $pdo->prepare('DELETE FROM community_group_bans WHERE group_id=? AND user_id=?')->execute([$gid,$target]);
    }
}
$pdo->prepare('INSERT INTO community_moderation_actions(actor_user_id,group_id,message_id,target_user_id,action) VALUES(?,?,?,?,?)')
    ->execute([$uid,$gid,$mid?:null,$target?:null,$action]);
community_json(200,['ok'=>true,'action'=>$action]);
