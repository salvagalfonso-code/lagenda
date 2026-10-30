<?php
declare(strict_types=1);
require_once __DIR__ . '/_community.php';
$user=community_user(); $uid=(int)$user['id']; $gid=(int)($_GET['group_id']??0); community_require_accepted($gid,$uid);
$stmt=community_pdo()->prepare("SELECT m.id,m.group_id,m.user_id,m.body,m.created_at,COALESCE(NULLIF(up.display_name,''),u.username) author,up.photo_url FROM community_messages m JOIN users u ON u.id=m.user_id LEFT JOIN user_profiles up ON up.user_id=u.id WHERE m.group_id=? AND m.deleted_at IS NULL
    AND NOT EXISTS (SELECT 1 FROM community_user_blocks b WHERE
        (b.blocker_user_id=? AND b.blocked_user_id=m.user_id) OR
        (b.blocker_user_id=m.user_id AND b.blocked_user_id=?))
    AND NOT EXISTS (SELECT 1 FROM community_group_bans gb WHERE gb.group_id=m.group_id AND gb.user_id=m.user_id)
    ORDER BY m.id ASC LIMIT 500");
$stmt->execute([$gid,$uid,$uid]); $messages=array_map('community_message_payload',$stmt->fetchAll()); community_json(200,['ok'=>true,'messages'=>$messages]);
