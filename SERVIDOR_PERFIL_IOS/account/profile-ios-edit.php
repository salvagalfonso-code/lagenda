<?php
declare(strict_types=1);
require_once __DIR__ . '/../images/_images.php';
if ($_SERVER['REQUEST_METHOD'] !== 'POST') fail('Mètode no permés', 405);
$user = current_user(true);
$data = request_data();
$name = trim((string)($data['display_name'] ?? ''));
$bio = trim((string)($data['bio'] ?? ''));
if ($name === '' || mb_strlen($name) > 190 || mb_strlen($bio) > 3000) fail('Nom o biografia no vàlids',422);
$pdo = db();
$pdo->beginTransaction();
try {
    // Narrow edit: retain home town, professional role, discipline, books, spaces, etc.
    $pdo->prepare('INSERT INTO user_profiles(user_id,display_name,bio,created_at,updated_at) VALUES(?,?,?,UTC_TIMESTAMP(),UTC_TIMESTAMP()) ON DUPLICATE KEY UPDATE display_name=VALUES(display_name),bio=VALUES(bio),updated_at=UTC_TIMESTAMP()')
        ->execute([(int)$user['id'],$name,$bio]);
    if (!empty($data['remove_image'])) img_remove_url('USER',(string)$user['username']);
    $pdo->commit();
} catch(Throwable $e){ if($pdo->inTransaction()) $pdo->rollBack(); throw $e; }
$photo = (string)(img_owner_url('USER',(string)$user['username']) ?: '');
send_json(['ok'=>true,'profile'=>['display_name'=>$name,'bio'=>$bio,'image_url'=>$photo]]);
