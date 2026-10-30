<?php
declare(strict_types=1);
require_once __DIR__ . '/../_bootstrap.php';
if ($_SERVER['REQUEST_METHOD'] !== 'POST') fail('Mètode no permés',405);
$user = current_user(true);
$data = request_data();
if (($data['confirmation'] ?? '') !== 'ELIMINAR') fail('Falta confirmar ELIMINAR',422,'CONFIRM_REQUIRED');
$id = (int)$user['id'];
$pdo = db();
$q = $pdo->prepare('SELECT id,role,password_hash FROM users WHERE id=? AND active=1 LIMIT 1');
$q->execute([$id]); $account = $q->fetch(PDO::FETCH_ASSOC);
if (!$account) fail('Compte no disponible',404,'NOT_FOUND');
if (($account['role'] ?? '') === 'SUPER_ADMIN') fail('Cal transferir el rol de superadministrador abans de la baixa',409,'ADMIN_TRANSFER_REQUIRED');
$hash = (string)($account['password_hash'] ?? '');
if ($hash !== '' && !password_verify((string)($data['password'] ?? ''), $hash)) {
    fail('Per eliminar el compte cal la contrasenya actual',403,'PASSWORD_REQUIRED');
}
function ios_delete_table(PDO $pdo, string $table, string $column, int $uid): void {
    // Static allowlist. No table or column names from the request.
    $check = $pdo->prepare('SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=? LIMIT 1');
    $check->execute([$table,$column]);
    if ($check->fetchColumn()) $pdo->prepare("DELETE FROM `$table` WHERE `$column`=?")->execute([$uid]);
}
// Financial transactions and moderation audit records remain where statutory retention is required.
// In particular do not cascade-delete a ticket order, an event owned by a promoter or sales records.
// Capture an owned local image for safe best-effort deletion after the DB commit.
$photoRow = $pdo->prepare('SELECT photo_url FROM user_profiles WHERE user_id=? LIMIT 1');
$photoRow->execute([$id]);
$photoPath = (string)($photoRow->fetchColumn() ?: '');
$pdo->beginTransaction();
try {
    $q = $pdo->prepare('SELECT id FROM users WHERE id=? AND active=1 FOR UPDATE');
    $q->execute([$id]);
    if (!$q->fetchColumn()) fail('Compte ja donat de baixa',409,'ALREADY_DELETED');
    foreach ([
        ['api_tokens','user_id'], ['user_google_accounts','user_id'],['user_apple_accounts','user_id'],
        ['user_profiles','user_id'],['profiles','owner_user_id'],['user_follows','user_id'],
        ['agenda_user_location_preferences','user_id'],['agenda_user_saved_search_filters','user_id'],
        ['agenda_saved_list_members','user_id'],['agenda_saved_lists','owner_user_id'],
        ['ios_artist_bells','user_id'],['ios_apns_devices','user_id'],['ios_artist_alert_pending','user_id'],
        ['community_user_blocks','blocker_user_id'],['community_user_blocks','blocked_user_id'],
        ['community_group_bans','user_id'],
        ['user_activity','user_id'],['user_affinities','user_id'],['user_batec_views','user_id'],
        ['user_event_states','user_id'],['user_event_alerts','user_id'],
        ['user_notification_preferences','user_id'],['user_professional_settings','user_id'],['user_event_states','user_id'],
        ['user_preferences','user_id'],['user_saved_events','user_id'],['saved_search_filters','user_id'],
        ['user_search_filters','user_id'],['user_event_alerts','user_id'],['notification_settings','user_id'],
        ['notification_recipients','user_id'],['user_notifications','user_id'],['event_reminders','user_id']
    ] as [$table,$column]) ios_delete_table($pdo,$table,$column,$id);
    // Prefer hard deletion: InnoDB applies defined CASCADE / SET NULL links.
    // If an event or fiscal record uses a restrictive foreign key, preserve that link
    // to an anonymised, permanently inactive account rather than violating the ledger.
    $mode = 'DELETED';
    try {
        $pdo->prepare('DELETE FROM users WHERE id=?')->execute([$id]);
    } catch (PDOException $foreign) {
        if ($foreign->getCode() !== '23000') throw $foreign;
        $mode = 'ANONYMIZED_LEGAL_RETENTION';
        $anon = 'eliminat_' . $id . '_' . substr(bin2hex(random_bytes(6)),0,12);
        $pdo->prepare("UPDATE users SET username=?, email=NULL,password_hash=NULL, email_verified_at=NULL,active=0,updated_at=UTC_TIMESTAMP() WHERE id=?")
            ->execute([$anon,$id]);
    }
    $pdo->commit();
    // Only locally owned profile images below the designated uploads path are unlinked.
    if (preg_match('~^images/profiles/[a-f0-9]{16}/[A-Za-z0-9_.-]+$~', $photoPath)) {
        $root = realpath(dirname(__DIR__,2) . '/images/profiles');
        $file = realpath(dirname(__DIR__,2) . '/' . $photoPath);
        if ($root && $file && str_starts_with($file, $root . DIRECTORY_SEPARATOR)) @unlink($file);
    }
    send_json(['ok'=>true,'deleted'=>true,'mode'=>$mode,
        'message'=>$mode==='DELETED' ? 'Compte eliminat definitivament.' : 'Compte desactivat i anonimitzat; es conserven únicament referències exigides per les dades vinculades.']);
} catch (Throwable $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    throw $e;
}
