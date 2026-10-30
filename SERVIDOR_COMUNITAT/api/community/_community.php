<?php
declare(strict_types=1);
require_once __DIR__ . '/../mail/bootstrap.php';

function community_pdo(): PDO { return agenda_mail_pdo(); }
function community_user(): array { return agenda_require_auth_user(); }
function community_json(int $status, array $payload): never { agenda_json_response($status, $payload); }
function community_input(): array { return agenda_read_json(); }

function community_group_type(string $value): string {
    $value = strtoupper(trim($value));
    return in_array($value, ['CAR','TRANSPORT','MEETUP','HOSTING','OTHER'], true) ? $value : 'OTHER';
}
function community_access_mode(string $value): string {
    return strtoupper(trim($value)) === 'APPROVAL' ? 'APPROVAL' : 'OPEN';
}
function community_event_archived(array $row): bool {
    $date = trim((string)($row['event_date'] ?? ''));
    if ($date === '') return false;
    $event = strtotime($date . ' 23:59:59');
    return $event !== false && $event < strtotime('-1 day');
}
function community_group_base(int $groupId): ?array {
    $pdo = community_pdo();
    $stmt = $pdo->prepare("SELECT g.*, e.title AS event_title, e.event_date, e.event_time,
             COALESCE(NULLIF(up.display_name,''), u.username) AS creator_name
        FROM community_groups g
        JOIN events e ON e.id=g.event_id
        JOIN users u ON u.id=g.creator_user_id
        LEFT JOIN user_profiles up ON up.user_id=u.id
        WHERE g.id=? LIMIT 1");
    $stmt->execute([$groupId]);
    $row = $stmt->fetch();
    return $row ?: null;
}
function community_refresh_full_status(int $groupId): void {
    $pdo = community_pdo();
    $row = community_group_base($groupId);
    if (!$row) return;
    if (community_event_archived($row)) {
        $pdo->prepare("UPDATE community_groups SET status='ARCHIVED' WHERE id=? AND status NOT IN ('CANCELLED','ARCHIVED')")->execute([$groupId]);
        return;
    }
    if (in_array((string)$row['status'], ['CANCELLED','CLOSED','ARCHIVED'], true)) return;
    $capacity = $row['capacity'] !== null ? (int)$row['capacity'] : null;
    if ($capacity === null || $capacity <= 0) {
        $pdo->prepare("UPDATE community_groups SET status='OPEN' WHERE id=? AND status='FULL'")->execute([$groupId]);
        return;
    }
    $stmt = $pdo->prepare("SELECT COUNT(*) FROM community_members WHERE group_id=? AND role='MEMBER' AND status='ACCEPTED'");
    $stmt->execute([$groupId]);
    $used = (int)$stmt->fetchColumn();
    $pdo->prepare("UPDATE community_groups SET status=? WHERE id=? AND status IN ('OPEN','FULL')")
        ->execute([$used >= $capacity ? 'FULL' : 'OPEN', $groupId]);
}
function community_assert_active(array $group): void {
    if (community_event_archived($group) || in_array((string)$group['status'], ['ARCHIVED','CANCELLED','CLOSED'], true)) {
        community_json(409, ['ok'=>false,'message'=>'Este grup ja no està actiu.']);
    }
}
function community_member_row(int $groupId, int $userId): ?array {
    $stmt = community_pdo()->prepare("SELECT * FROM community_members WHERE group_id=? AND user_id=? LIMIT 1");
    $stmt->execute([$groupId,$userId]);
    $row = $stmt->fetch();
    return $row ?: null;
}
function community_require_creator(int $groupId, int $userId): array {
    $group = community_group_base($groupId);
    if (!$group) community_json(404,['ok'=>false,'message'=>'Grup no trobat.']);
    if ((int)$group['creator_user_id'] !== $userId) community_json(403,['ok'=>false,'message'=>'Només el creador pot gestionar este grup.']);
    return $group;
}
function community_require_accepted(int $groupId, int $userId): array {
    $group = community_group_base($groupId);
    if (!$group) community_json(404,['ok'=>false,'message'=>'Grup no trobat.']);
    $member = community_member_row($groupId,$userId);
    if (!$member || (string)$member['status'] !== 'ACCEPTED') community_json(403,['ok'=>false,'message'=>'Has d’estar acceptat/da al grup.']);
    return $group;
}
function community_members_payload(int $groupId): array {
    $pdo = community_pdo();
    $stmt = $pdo->prepare("SELECT m.user_id,m.role,m.status,m.requested_at,
              COALESCE(NULLIF(up.display_name,''),u.username) AS display_name,
              up.photo_url
        FROM community_members m
        JOIN users u ON u.id=m.user_id
        LEFT JOIN user_profiles up ON up.user_id=u.id
        WHERE m.group_id=? AND m.status='ACCEPTED'
        ORDER BY (m.role='CREATOR') DESC,m.requested_at ASC");
    $stmt->execute([$groupId]);
    return array_map(static fn(array $m): array => [
        'user_id'=>(int)$m['user_id'],'display_name'=>(string)$m['display_name'],
        'photo_url'=>$m['photo_url'] ?: null,'role'=>(string)$m['role'],'status'=>(string)$m['status']
    ], $stmt->fetchAll());
}
function community_group_payload(int $groupId, int $viewerUserId): array {
    community_refresh_full_status($groupId);
    $row = community_group_base($groupId);
    if (!$row) community_json(404,['ok'=>false,'message'=>'Grup no trobat.']);
    $pdo = community_pdo();
    $count = $pdo->prepare("SELECT
       SUM(status='ACCEPTED') accepted_count,
       SUM(status='ACCEPTED' AND role='MEMBER') participant_count,
       SUM(status='PENDING') pending_count
       FROM community_members WHERE group_id=?");
    $count->execute([$groupId]);
    $c = $count->fetch() ?: [];
    $mine = community_member_row($groupId,$viewerUserId);
    $messageCountStmt = $pdo->prepare("SELECT COUNT(*) FROM community_messages WHERE group_id=? AND user_id=? AND deleted_at IS NULL");
    $messageCountStmt->execute([$groupId,$viewerUserId]);
    $myMessageCount = (int)$messageCountStmt->fetchColumn();
    return [
        'id'=>(int)$row['id'],'event_id'=>(int)$row['event_id'],'event_title'=>(string)$row['event_title'],
        'creator_user_id'=>(int)$row['creator_user_id'],'creator_name'=>(string)$row['creator_name'],
        'type'=>(string)$row['group_type'],'access_mode'=>(string)$row['access_mode'],
        'title'=>(string)$row['title'],'description'=>(string)($row['description'] ?? ''),
        'origin_label'=>(string)($row['origin_label'] ?? ''),'meeting_at'=>$row['meeting_at'] ?: null,
        'capacity'=>$row['capacity'] !== null ? (int)$row['capacity'] : null,
        'status'=>(string)$row['status'],'member_count'=>(int)($c['accepted_count'] ?? 0),
        'participant_count'=>(int)($c['participant_count'] ?? 0),'pending_count'=>(int)($c['pending_count'] ?? 0),
        'my_status'=>$mine ? (string)$mine['status'] : 'NONE','my_role'=>$mine ? (string)$mine['role'] : '',
        'my_message_count'=>$myMessageCount,
        'members'=>community_members_payload($groupId),'created_at'=>(string)$row['created_at']
    ];
}
function community_message_payload(array $row): array {
    return [
        'id'=>(int)$row['id'],'group_id'=>(int)$row['group_id'],'user_id'=>(int)$row['user_id'],
        'author'=>(string)$row['author'],'photo_url'=>$row['photo_url'] ?: null,'text'=>(string)$row['body'],
        'created_at'=>(string)$row['created_at']
    ];
}
