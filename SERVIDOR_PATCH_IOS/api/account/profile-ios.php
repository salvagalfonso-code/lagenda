<?php
declare(strict_types=1);
/** Deploy to app/api/account/profile-ios.php. Requires existing user_profiles schema. */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/../images/_images.php';
if ($_SERVER['REQUEST_METHOD'] !== 'GET') fail('Mètode no permés.', 405, 'METHOD_NOT_ALLOWED');
$user = current_user(true);
$uid = (int)$user['id'];
$stmt = db()->prepare('SELECT display_name, bio, home_town, photo_url FROM user_profiles WHERE user_id=? LIMIT 1');
$stmt->execute([$uid]);
$profile = $stmt->fetch(PDO::FETCH_ASSOC) ?: [];
$display = trim((string)($profile['display_name'] ?? ''));
if ($display === '') $display = (string)$user['username'];
$image = img_owner_url('USER', strtolower((string)$user['username']));
send_json(['ok' => true, 'profile' => [
    'display_name' => $display,
    'bio' => (string)($profile['bio'] ?? ''),
    'home_town' => (string)($profile['home_town'] ?? ''),
    'image_url' => $image,
]]);
