<?php
declare(strict_types=1);
require_once __DIR__ . '/_community.php';
if ($_SERVER['REQUEST_METHOD']!=='POST') community_json(405,['ok'=>false,'message'=>'Mètode no permés.']);
$user=community_user(); $uid=(int)$user['id']; $in=community_input();
$gid=(int)($in['group_id']??0); $mid=(int)($in['message_id']??0);
$target=(int)($in['reported_user_id']??0);
$reason=mb_substr(trim((string)($in['reason']??'')),0,500);
if (!$gid || $reason==='') community_json(422,['ok'=>false,'message'=>'Falten dades de la denúncia.']);
$group=community_group_base($gid);
if (!$group) community_json(404,['ok'=>false,'message'=>'Tauló no trobat.']);
$pdo=community_pdo();
if ($mid>0) {
    community_require_accepted($gid,$uid);
    $q=$pdo->prepare('SELECT user_id FROM community_messages WHERE group_id=? AND id=? AND deleted_at IS NULL');
    $q->execute([$gid,$mid]); $actual=$q->fetchColumn();
    if (!$actual) community_json(404,['ok'=>false,'message'=>'Missatge no trobat.']);
    $target=(int)$actual;
} elseif ($target>0) {
    $q=$pdo->prepare('SELECT 1 FROM community_members WHERE group_id=? AND user_id=?');
    $q->execute([$gid,$target]);
    if (!$q->fetchColumn()) community_json(422,['ok'=>false,'message'=>'Usuari no vinculat al tauló.']);
} else $target=0;
$q=$pdo->prepare('SELECT COUNT(*) FROM community_reports WHERE reporter_user_id=? AND created_at>=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 DAY)');
$q->execute([$uid]);
if ((int)$q->fetchColumn()>=20) community_json(429,['ok'=>false,'message'=>'Límit temporal de denúncies assolit.']);
$pdo->prepare('INSERT INTO community_reports(reporter_user_id,group_id,message_id,reported_user_id,reason) VALUES(?,?,?,?,?)')
  ->execute([$uid,$gid,$mid?:null,$target?:null,$reason]);
community_json(201,['ok'=>true]);
