<?php
declare(strict_types=1);
require_once __DIR__ . '/_community.php';
if ($_SERVER['REQUEST_METHOD']!=='POST') community_json(405,['ok'=>false,'message'=>'Mètode no permés.']);
$user=community_user(); $uid=(int)$user['id']; $in=community_input(); $gid=(int)($in['group_id']??0);
$group=community_group_base($gid); if(!$group) community_json(404,['ok'=>false,'message'=>'Grup no trobat.']); community_assert_active($group); community_refresh_full_status($gid); $group=community_group_base($gid);
if((int)$group['creator_user_id']===$uid) community_json(200,['ok'=>true,'membership_status'=>'ACCEPTED','group'=>community_group_payload($gid,$uid)]);
$pdo=community_pdo();
$ban=$pdo->prepare('SELECT 1 FROM community_group_bans WHERE group_id=? AND user_id=?');
$ban->execute([$gid,$uid]);
if ($ban->fetchColumn()) community_json(403,['ok'=>false,'message'=>'No tens accés a este tauló.']);
$block=$pdo->prepare('SELECT 1 FROM community_user_blocks WHERE
   (blocker_user_id=? AND blocked_user_id=?) OR (blocker_user_id=? AND blocked_user_id=?) LIMIT 1');
$creator=(int)$group['creator_user_id']; $block->execute([$uid,$creator,$creator,$uid]);
if ($block->fetchColumn()) community_json(403,['ok'=>false,'message'=>'No pots unir-te a este tauló.']);
$existing=community_member_row($gid,$uid); if($existing && $existing['status']==='ACCEPTED') community_json(200,['ok'=>true,'membership_status'=>'ACCEPTED','group'=>community_group_payload($gid,$uid)]);
if((string)$group['status']==='FULL') community_json(409,['ok'=>false,'message'=>'El grup està complet.']);
$status=((string)$group['access_mode']==='OPEN')?'ACCEPTED':'PENDING';
$pdo=community_pdo();
$stmt=$pdo->prepare("INSERT INTO community_members(group_id,user_id,role,status,requested_at,responded_at) VALUES(?,?,'MEMBER',?,NOW(),IF(?='ACCEPTED',NOW(),NULL)) ON DUPLICATE KEY UPDATE role='MEMBER',status=VALUES(status),requested_at=NOW(),responded_at=VALUES(responded_at)");
$stmt->execute([$gid,$uid,$status,$status]); community_refresh_full_status($gid);
community_json(200,['ok'=>true,'membership_status'=>$status,'group'=>community_group_payload($gid,$uid)]);
