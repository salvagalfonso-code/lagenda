<?php
declare(strict_types=1);
require __DIR__ . '/../_bootstrap.php';
$user = current_user(true);
$uid = (int)$user['id'];
$pdo = db();
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $q = $pdo->prepare('SELECT artist_name,frequency,enabled FROM ios_artist_bells WHERE user_id=? AND enabled=1 ORDER BY artist_name');
    $q->execute([$uid]);
    send_json(['ok'=>true,'artists'=>$q->fetchAll()]);
}
if ($_SERVER['REQUEST_METHOD'] !== 'POST') fail('Mètode no permés.',405,'METHOD_NOT_ALLOWED');
$data = request_data();
$name = clean_string((string)($data['artist_name'] ?? ''),190);
$key = preg_replace('/\s+/u',' ',trim($name));
$key = function_exists('mb_strtolower') ? mb_strtolower($key,'UTF-8') : strtolower($key);
if ($key === '') fail('Falta el nom de l’artista.',422,'VALIDATION');
$frequency = strtoupper(trim((string)($data['frequency'] ?? 'WEEKLY')));
if (!in_array($frequency, ['IMMEDIATE','DAILY','WEEKLY'], true)) fail('Freqüència no vàlida.',422,'VALIDATION');
$enabled = filter_var($data['enabled'] ?? false, FILTER_VALIDATE_BOOLEAN);
if ($enabled) {
    // A frequency change must not notify old events. An existing subscription retains its activation date.
    $pdo->prepare("INSERT INTO ios_artist_bells(user_id,artist_key,artist_name,frequency,enabled,activated_at)
        VALUES(?,?,?,?,1,UTC_TIMESTAMP()) ON DUPLICATE KEY UPDATE
        artist_name=VALUES(artist_name),frequency=VALUES(frequency),
        activated_at=IF(enabled=1,activated_at,UTC_TIMESTAMP()),enabled=1,updated_at=UTC_TIMESTAMP()")
        ->execute([$uid,$key,$name,$frequency]);
} else {
    // Disabled bells cannot keep queued notifications that might be sent after disabling.
    $pdo->beginTransaction();
    try {
        $pdo->prepare('UPDATE ios_artist_bells SET enabled=0, updated_at=UTC_TIMESTAMP() WHERE user_id=? AND artist_key=?')
            ->execute([$uid,$key]);
        $pdo->prepare('DELETE FROM ios_artist_alert_pending WHERE user_id=? AND artist_key=? AND delivered_at IS NULL')
            ->execute([$uid,$key]);
        $pdo->commit();
    } catch (Throwable $e) { if ($pdo->inTransaction()) $pdo->rollBack(); throw $e; }
}
send_json(['ok'=>true,'artist_name'=>$name,'frequency'=>$frequency,'enabled'=>$enabled]);
