<?php
declare(strict_types=1);
require_once __DIR__ . '/_community.php';
$user=community_user(); $uid=(int)$user['id'];
if (strtoupper((string)($user['role']??''))!=='SUPER_ADMIN') community_json(403,['ok'=>false,'message'=>'Només Super Admin.']);
$pdo=community_pdo();
if ($_SERVER['REQUEST_METHOD']==='GET') {
    $status=strtoupper((string)($_GET['status']??'OPEN'));
    if (!in_array($status,['OPEN','ALL'],true)) community_json(422,['ok'=>false,'message'=>'Estat invàlid.']);
    $sql="SELECT r.id,r.group_id,r.message_id,r.reported_user_id,r.reason,r.status,r.created_at,
       g.title AS group_title,m.body AS message_body,
       COALESCE(NULLIF(up.display_name,''),u.username) reported_name
       FROM community_reports r LEFT JOIN community_groups g ON g.id=r.group_id
       LEFT JOIN community_messages m ON m.id=r.message_id AND m.group_id=r.group_id
       LEFT JOIN users u ON u.id=r.reported_user_id LEFT JOIN user_profiles up ON up.user_id=u.id";
    if ($status==='OPEN') $sql.=" WHERE r.status='OPEN'";
    $sql.=' ORDER BY r.created_at DESC LIMIT 200';
    $rows=$pdo->query($sql)->fetchAll();
    community_json(200,['ok'=>true,'reports'=>array_map(static fn($r)=>[
       'id'=>(int)$r['id'],'group_id'=>(int)$r['group_id'],'message_id'=>(int)$r['message_id'],
       'reported_user_id'=>(int)$r['reported_user_id'],'reported_name'=>(string)($r['reported_name']??''),
       'reason'=>(string)$r['reason'],'status'=>(string)$r['status'],'created_at'=>(string)$r['created_at'],
       'group_title'=>(string)($r['group_title']??''),'message_body'=>(string)($r['message_body']??'')
    ],$rows)]);
}
if ($_SERVER['REQUEST_METHOD']!=='POST') community_json(405,['ok'=>false,'message'=>'Mètode no permés.']);
$in=community_input(); $id=(int)($in['report_id']??0); $action=strtoupper(trim((string)($in['action']??'')));
if ($id<=0 || !in_array($action,['DISMISS','REVIEW','DELETE_MESSAGE','CLOSE_GROUP','BAN_MEMBER'],true)) community_json(422,['ok'=>false,'message'=>'Acció incorrecta.']);
$pdo->beginTransaction();
try {
    $q=$pdo->prepare('SELECT * FROM community_reports WHERE id=? FOR UPDATE');$q->execute([$id]);$r=$q->fetch();
    if (!$r) { $pdo->rollBack(); community_json(404,['ok'=>false,'message'=>'Denúncia no trobada.']); }
    if ($r['status']!=='OPEN') { $pdo->rollBack(); community_json(409,['ok'=>false,'message'=>'Denúncia ja revisada.']); }
    $gid=(int)$r['group_id']; $mid=(int)$r['message_id']; $target=(int)$r['reported_user_id'];
    if ($action==='DELETE_MESSAGE') {
        if ($gid<=0 || $mid<=0) { $pdo->rollBack(); community_json(422,['ok'=>false,'message'=>'Esta denúncia no té missatge.']); }
        $pdo->prepare('UPDATE community_messages SET deleted_at=UTC_TIMESTAMP() WHERE id=? AND group_id=?')->execute([$mid,$gid]);
    }
    if ($action==='CLOSE_GROUP') {
        if ($gid<=0) { $pdo->rollBack(); community_json(422,['ok'=>false,'message'=>'Esta denúncia no té tauló.']); }
        $pdo->prepare("UPDATE community_groups SET status='CLOSED' WHERE id=? AND status NOT IN ('CANCELLED','ARCHIVED')")->execute([$gid]);
    }
    if ($action==='BAN_MEMBER') {
        if ($gid<=0 || $target<=0) { $pdo->rollBack(); community_json(422,['ok'=>false,'message'=>'Falta la persona denunciada.']); }
        $q=$pdo->prepare('SELECT creator_user_id FROM community_groups WHERE id=?'); $q->execute([$gid]);
        if ((int)$q->fetchColumn()===$target) { $pdo->rollBack(); community_json(422,['ok'=>false,'message'=>'No pots expulsar el creador; tanca el tauló.']); }
        $member=$pdo->prepare("SELECT 1 FROM community_members WHERE group_id=? AND user_id=? AND role='MEMBER'");
        $member->execute([$gid,$target]);
        if (!$member->fetchColumn()) { $pdo->rollBack(); community_json(422,['ok'=>false,'message'=>'La persona denunciada no pertany al tauló.']); }
        $pdo->prepare('INSERT INTO community_group_bans(group_id,user_id,banned_by_user_id) VALUES(?,?,?) ON DUPLICATE KEY UPDATE banned_by_user_id=VALUES(banned_by_user_id)')->execute([$gid,$target,$uid]);
        $pdo->prepare("UPDATE community_members SET status='REJECTED' WHERE group_id=? AND user_id=? AND role='MEMBER'")->execute([$gid,$target]);
    }
    $newStatus=$action==='DISMISS'?'DISMISSED':($action==='REVIEW'?'REVIEWED':'ACTIONED');
    $pdo->prepare('UPDATE community_reports SET status=?,reviewed_at=UTC_TIMESTAMP() WHERE id=?')->execute([$newStatus,$id]);
    $pdo->prepare('INSERT INTO community_moderation_actions(actor_user_id,group_id,message_id,target_user_id,report_id,action) VALUES(?,?,?,?,?,?)')
       ->execute([$uid,$gid?:null,$mid?:null,$target?:null,$id,$action]);
    $pdo->commit(); community_json(200,['ok'=>true,'status'=>$newStatus]);
} catch (Throwable $ex) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    error_log('[Community admin moderation] '.$ex->getMessage());
    community_json(500,['ok'=>false,'message'=>'No s\'ha pogut moderar.']);
}
