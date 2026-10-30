<?php
/** Change role/status only. Unlike user-update.php, this does not touch user_profiles. */
declare(strict_types=1);
require __DIR__ . '/../_bootstrap.php';
if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') fail('Mètode no permés.', 405, 'METHOD_NOT_ALLOWED');
$me = current_user(true);
if (($me['role'] ?? '') !== 'SUPER_ADMIN' || !(bool)($me['active'] ?? true)) {
    fail('Accés restringit a Super Admin.', 403, 'ROLE_FORBIDDEN');
}
$data = request_data();
$id = (int)($data['user_id'] ?? 0);
$role = strtoupper(trim((string)($data['role'] ?? '')));
$roles = ['PUBLIC','USER','GROUP','ORGANIZER','VENUE','PROMOTER','LABEL','AGENCY','SUPER_ADMIN'];
if ($id <= 0 || !in_array($role, $roles, true) || !array_key_exists('active', $data) || !is_bool($data['active'])) {
    fail('ID, rol o activació incorrectes.', 422, 'VALIDATION');
}
if (($data['confirmation'] ?? '') !== 'CANVIAR ACCES') fail('Falta la confirmació.', 422, 'CONFIRMATION_REQUIRED');
if ($id === (int)$me['id']) fail('No pots canviar el teu propi accés.', 409, 'SELF_LOCKOUT');
$active = $data['active'];
$pdo = db();
$pdo->beginTransaction();
try {
    $q = $pdo->prepare('SELECT id,role,active FROM users WHERE id=? FOR UPDATE');
    $q->execute([$id]);
    $old = $q->fetch(PDO::FETCH_ASSOC);
    if (!$old) { $pdo->rollBack(); fail('Usuari no trobat.', 404, 'NOT_FOUND'); }
    $check = $pdo->prepare("SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='agenda_admin_access_audit' LIMIT 1");
    $check->execute();
    if (!$check->fetchColumn()) {
        $pdo->rollBack(); fail('Cal aplicar la migració 001_SUPERADMIN_IOS.sql.', 409, 'MIGRATION_REQUIRED');
    }
    // A server-side session is required for every administrative request.
    $pdo->prepare('UPDATE users SET role=?, active=?, updated_at=UTC_TIMESTAMP() WHERE id=?')
        ->execute([$role, $active ? 1 : 0, $id]);
    // Revocation is mandatory when removing privileges or disabling an account.
    $revoked = (!$active || $role !== (string)$old['role']);
    if ($revoked) {
        $pdo->prepare('DELETE FROM api_tokens WHERE user_id=?')->execute([$id]);
    }
    $pdo->prepare('INSERT INTO agenda_admin_access_audit (actor_user_id,target_user_id,previous_role,new_role,previous_active,new_active) VALUES(?,?,?,?,?,?)')
        ->execute([(int)$me['id'], $id, (string)$old['role'], $role, (int)$old['active'], $active ? 1 : 0]);
    $pdo->commit();
} catch (Throwable $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    error_log('[Agenda admin access iOS] ' . $e->getMessage());
    fail('No s’ha pogut actualitzar l’accés.', 500, 'ACCESS_UPDATE_FAILED');
}
send_json(['ok'=>true, 'user_id'=>$id, 'role'=>$role, 'active'=>$active, 'tokens_revoked'=>$revoked]);
