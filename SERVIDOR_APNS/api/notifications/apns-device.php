<?php
declare(strict_types=1);
require __DIR__ . '/../_bootstrap.php';
if ($_SERVER['REQUEST_METHOD'] !== 'POST') fail('Mètode no permés.',405,'METHOD_NOT_ALLOWED');
$user = current_user(true);
$input = request_data();
$action = strtolower(trim((string)($input['action'] ?? 'register')));
$token = strtolower(trim((string)($input['device_token'] ?? '')));
if (!preg_match('/^[0-9a-f]{64,128}$/D', $token)) fail('Token APNs no vàlid.',422,'VALIDATION');
$pdo = db();
if ($action === 'unregister') {
    $pdo->prepare('DELETE FROM ios_apns_devices WHERE user_id=? AND device_token=?')
        ->execute([(int)$user['id'],$token]);
    send_json(['ok'=>true,'registered'=>false]);
}
if ($action !== 'register') fail('Acció no vàlida.',422,'VALIDATION');
$environment = strtolower(trim((string)($input['environment'] ?? '')));
if (!in_array($environment,['development','production'],true)) fail('Entorn APNs no vàlid.',422,'VALIDATION');
// One APNs token belongs to exactly one account. Logging into another account reassigns it.
$pdo->prepare("INSERT INTO ios_apns_devices (user_id,device_token,environment,enabled,last_seen_at)
  VALUES(?,?,?,1,UTC_TIMESTAMP()) ON DUPLICATE KEY UPDATE
  user_id=VALUES(user_id),environment=VALUES(environment),enabled=1,last_seen_at=UTC_TIMESTAMP()")
  ->execute([(int)$user['id'],$token,$environment]);
send_json(['ok'=>true,'registered'=>true]);
