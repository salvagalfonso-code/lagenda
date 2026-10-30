<?php
declare(strict_types=1);
require_once __DIR__ . '/_community.php';
if ($_SERVER['REQUEST_METHOD']!=='POST') community_json(405,['ok'=>false,'message'=>'Mètode no permés.']);
$user=community_user(); $uid=(int)$user['id']; $in=community_input(); $gid=(int)($in['group_id']??0); $text=trim((string)($in['text']??'')); $group=community_require_accepted($gid,$uid); community_assert_active($group);
$pdo=community_pdo();
$ban=$pdo->prepare('SELECT 1 FROM community_group_bans WHERE group_id=? AND user_id=?');
$ban->execute([$gid,$uid]);
if ($ban->fetchColumn()) community_json(403,['ok'=>false,'message'=>'No pots escriure en este tauló.']);
$creator=(int)$group['creator_user_id'];
$block=$pdo->prepare('SELECT 1 FROM community_user_blocks WHERE
  (blocker_user_id=? AND blocked_user_id=?) OR (blocker_user_id=? AND blocked_user_id=?) LIMIT 1');
$block->execute([$uid,$creator,$creator,$uid]);
if ($block->fetchColumn()) community_json(403,['ok'=>false,'message'=>'No pots escriure en este tauló.']);
if($text==='') community_json(422,['ok'=>false,'message'=>'Escriu un missatge.']); $text=mb_substr($text,0,2000); $pdo=community_pdo(); $pdo->prepare('INSERT INTO community_messages(group_id,user_id,body) VALUES(?,?,?)')->execute([$gid,$uid,$text]); $mid=(int)$pdo->lastInsertId();
$stmt=$pdo->prepare("SELECT m.id,m.group_id,m.user_id,m.body,m.created_at,COALESCE(NULLIF(up.display_name,''),u.username) author,up.photo_url FROM community_messages m JOIN users u ON u.id=m.user_id LEFT JOIN user_profiles up ON up.user_id=u.id WHERE m.id=?"); $stmt->execute([$mid]);
community_json(201,['ok'=>true,'message'=>community_message_payload($stmt->fetch())]);
