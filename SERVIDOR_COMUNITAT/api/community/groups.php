<?php
declare(strict_types=1);
require_once __DIR__ . '/_community.php';
$user = community_user();
$uid = (int)$user['id'];
$eventId = max(0,(int)($_GET['event_id'] ?? 0));
$mine = ((int)($_GET['mine'] ?? 0)) === 1;
$pdo = community_pdo();
$sql = "SELECT DISTINCT g.id FROM community_groups g JOIN events e ON e.id=g.event_id";
$params=[]; $where=["g.status <> 'CANCELLED'","g.status <> 'ARCHIVED'"];
if ($mine) {
    $where[] = "(g.creator_user_id=? OR EXISTS (
        SELECT 1 FROM community_members cm
        WHERE cm.group_id=g.id AND cm.user_id=? AND cm.status='ACCEPTED'
          AND (g.access_mode='APPROVAL' OR (g.access_mode='OPEN' AND EXISTS (
              SELECT 1 FROM community_messages msg
              WHERE msg.group_id=g.id AND msg.user_id=? AND msg.deleted_at IS NULL
          )))
    ))";
    $params[]=$uid; $params[]=$uid; $params[]=$uid;
}
// No oferir taulons creats per persones bloquejades o que t'han bloquejat.
$where[] = "NOT EXISTS (SELECT 1 FROM community_user_blocks b WHERE
  (b.blocker_user_id=? AND b.blocked_user_id=g.creator_user_id) OR
  (b.blocker_user_id=g.creator_user_id AND b.blocked_user_id=?))";
$params[]=$uid; $params[]=$uid;
if ($eventId>0) { $where[]='g.event_id=?'; $params[]=$eventId; }
$sql .= ' WHERE '.implode(' AND ',$where).' ORDER BY COALESCE(g.meeting_at,g.created_at) ASC,g.id DESC LIMIT 200';
$stmt=$pdo->prepare($sql); $stmt->execute($params);
$groups=[]; foreach($stmt->fetchAll() as $r){ $payload=community_group_payload((int)$r['id'],$uid); if($payload['status']!=='ARCHIVED') $groups[]=$payload; }
community_json(200,['ok'=>true,'groups'=>$groups]);
