<?php
declare(strict_types=1);
/** Deploy to app/api/account/profile-ios-edit.php. Only edits current authenticated user's public profile. */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/../images/_images.php';
if ($_SERVER['REQUEST_METHOD'] !== 'POST') fail('Mètode no permés.', 405, 'METHOD_NOT_ALLOWED');
$user = current_user(true);
$uid = (int)$user['id'];
$data = request_data();
$display = trim((string)($data['display_name'] ?? ''));
$bio = trim((string)($data['bio'] ?? ''));
if ($display === '' || mb_strlen($display, 'UTF-8') > 190) {
    fail('El nom de perfil ha de tindre entre 1 i 190 caràcters.', 422, 'VALIDATION');
}
if (mb_strlen($bio, 'UTF-8') > 3000) fail('La biografia supera els 3000 caràcters.', 422, 'VALIDATION');
$removeImage = ($data['remove_image'] ?? false) === true;
$pdo = db();
$pdo->beginTransaction();
try {
    $pdo->prepare('INSERT INTO user_profiles(user_id,display_name,bio,created_at,updated_at)
        VALUES(?,?,?,UTC_TIMESTAMP(),UTC_TIMESTAMP()) ON DUPLICATE KEY UPDATE
        display_name=VALUES(display_name), bio=VALUES(bio), updated_at=UTC_TIMESTAMP()')
        ->execute([$uid, $display, $bio]);
    if ($removeImage) img_remove_url('USER', strtolower((string)$user['username']));
    $pdo->commit();
} catch (Throwable $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    error_log('[Agenda iOS profile edit] database error');
    fail('No s’ha pogut guardar el perfil.', 500, 'PROFILE_SAVE_FAILED');
}
$image = img_owner_url('USER', strtolower((string)$user['username']));
send_json(['ok'=>true, 'profile'=>[
    'display_name'=>$display,
    'bio'=>$bio,
    'image_url'=>$image,
]]);
