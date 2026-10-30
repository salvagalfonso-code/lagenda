<?php
declare(strict_types=1);
require_once __DIR__ . '/../images/_images.php';
if ($_SERVER['REQUEST_METHOD'] !== 'GET') fail('Mètode no permés', 405);
$user = current_user(true);
$stmt = db()->prepare('SELECT display_name,bio,photo_url FROM user_profiles WHERE user_id=? LIMIT 1');
$stmt->execute([(int)$user['id']]);
$profile = $stmt->fetch(PDO::FETCH_ASSOC) ?: [];
$photo = (string)($profile['photo_url'] ?? '');
if ($photo === '') $photo = (string)(img_owner_url('USER', (string)$user['username']) ?: '');
send_json(['ok'=>true,'profile'=>[
    'display_name'=>(string)((trim((string)($profile['display_name'] ?? '')) ?: $user['username'])),
    'bio'=>(string)($profile['bio'] ?? ''),
    'image_url'=>$photo
]]);
