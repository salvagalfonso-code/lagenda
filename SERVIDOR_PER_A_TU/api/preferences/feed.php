<?php
declare(strict_types=1);
require_once __DIR__.'/../_bootstrap.php';
// The existing authenticated account owns its settings; callers never submit a user_id.
$user = current_user(true);
$userId = (int)$user['id'];
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
if (!in_array($method, ['GET','POST'], true)) fail('Mètode no permés.', 405, 'METHOD_NOT_ALLOWED');
try {
    $pdo = db();
    if ($method === 'POST') {
        $raw = request_data();
        $options = $raw['preferences'] ?? $raw;
        if (!is_array($options) || !array_key_exists('suggestions', $options) ||
            !array_key_exists('discovery', $options) || !array_key_exists('valencian_first', $options) ||
            !is_bool($options['suggestions']) || !is_bool($options['valencian_first']) ||
            !is_string($options['discovery']) ||
            !in_array($options['discovery'], ['MY_TASTES','BALANCED','EXPLORE'], true)) {
            fail('Preferències del feed invàlides.', 422, 'VALIDATION');
        }
        $stmt = $pdo->prepare('INSERT INTO user_for_you_preferences
            (user_id,suggestions,discovery,valencian_first,updated_at)
            VALUES (?,?,?,?,UTC_TIMESTAMP()) ON DUPLICATE KEY UPDATE
            suggestions=VALUES(suggestions),discovery=VALUES(discovery),
            valencian_first=VALUES(valencian_first),updated_at=UTC_TIMESTAMP()');
        $stmt->execute([$userId,(int)$options['suggestions'],$options['discovery'],(int)$options['valencian_first']]);
        // The saved preferences are the canonical cache key. Remove any older selection.
        try {
            $pdo->prepare('DELETE FROM for_you_server_cache WHERE user_id=?')->execute([$userId]);
        } catch (Throwable $e) { error_log('for-you cache invalidate: '.$e->getMessage()); }
    }
    $stmt = $pdo->prepare('SELECT suggestions,discovery,valencian_first FROM user_for_you_preferences WHERE user_id=? LIMIT 1');
    $stmt->execute([$userId]);
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    send_json(['ok'=>true,'exists'=>$row!==false,'preferences'=>[
        'suggestions'=>$row!==false ? (bool)$row['suggestions'] : true,
        'discovery'=>$row!==false ? $row['discovery'] : 'BALANCED',
        'valencian_first'=>$row!==false ? (bool)$row['valencian_first'] : true,
    ]]);
} catch (Throwable $e) {
    error_log('preferences/feed.php '.$e->getMessage());
    fail('No s’han pogut sincronitzar les preferències de Per a tu.',503,'PREFERENCES_SERVER_ERROR');
}
