<?php
declare(strict_types=1);
require_once __DIR__ . '/../_bootstrap.php';
if ($_SERVER['REQUEST_METHOD'] !== 'POST') fail('Mètode no permés', 405, 'METHOD_NOT_ALLOWED');
header('Cache-Control: no-store');

function apple_b64(string $part): string|false {
    return base64_decode(strtr($part, '-_', '+/') . str_repeat('=', (4 - strlen($part) % 4) % 4), true);
}
function apple_der_len(int $n): string {
    if ($n < 128) return chr($n);
    $s = ltrim(pack('N', $n), "\0");
    return chr(0x80 | strlen($s)) . $s;
}
function apple_der(int $tag, string $body): string { return chr($tag) . apple_der_len(strlen($body)) . $body; }
function apple_rsa_pem(string $modulus, string $exponent): string {
    // X.509 SubjectPublicKeyInfo for Apple's RSA JWK (not a client-controlled key).
    foreach (['modulus' => &$modulus, 'exponent' => &$exponent] as &$part) {
        $part = ltrim($part, "\0");
        if ($part === '' || (ord($part[0]) & 0x80)) $part = "\0" . $part;
    }
    unset($part);
    $rsa = apple_der(0x30, apple_der(0x02, $modulus) . apple_der(0x02, $exponent));
    $alg = hex2bin('300d06092a864886f70d0101010500');
    $spki = apple_der(0x30, $alg . apple_der(0x03, "\0" . $rsa));
    return "-----BEGIN PUBLIC KEY-----\n" . chunk_split(base64_encode($spki), 64, "\n") . "-----END PUBLIC KEY-----\n";
}
function apple_keys(): array {
    $file = sys_get_temp_dir() . '/agenda_apple_jwks.json';
    if (is_file($file) && time() - (int)filemtime($file) < 21600) {
        $cached = json_decode((string)file_get_contents($file), true);
        if (is_array($cached) && isset($cached['keys'])) return $cached['keys'];
    }
    $url = 'https://appleid.apple.com/auth/keys';
    $raw = false;
    $status = 0;
    if (function_exists('curl_init')) {
        $ch = curl_init($url);
        if ($ch) {
            curl_setopt_array($ch, [CURLOPT_RETURNTRANSFER => true, CURLOPT_CONNECTTIMEOUT => 5,
                CURLOPT_TIMEOUT => 10, CURLOPT_SSL_VERIFYPEER => true, CURLOPT_FOLLOWLOCATION => false]);
            $raw = curl_exec($ch);
            $status = (int)curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
            curl_close($ch);
        }
    } elseif (ini_get('allow_url_fopen')) {
        $context = stream_context_create(['http' => ['timeout'=>10,'follow_location'=>0],
            'ssl'=>['verify_peer'=>true,'verify_peer_name'=>true]]);
        $raw = @file_get_contents($url, false, $context);
        $status = is_string($raw) ? 200 : 0;
    }
    $parsed = is_string($raw) ? json_decode($raw, true) : null;
    if ($status !== 200 || !is_array($parsed) || empty($parsed['keys'])) fail('Claus d’Apple no disponibles', 503);
    file_put_contents($file, json_encode($parsed), LOCK_EX);
    return $parsed['keys'];
}
function apple_verify_jwt(string $jwt, string $nonce): array {
    $pieces = explode('.', $jwt);
    if (count($pieces) !== 3) fail('Token Apple invàlid', 401);
    [$head64, $body64, $sig64] = $pieces;
    $header = json_decode((string)apple_b64($head64), true);
    $claims = json_decode((string)apple_b64($body64), true);
    $signature = apple_b64($sig64);
    if (!is_array($header) || !is_array($claims) || $signature === false || ($header['alg'] ?? '') !== 'RS256') {
        fail('Token Apple no verificable', 401);
    }
    $kid = (string)($header['kid'] ?? '');
    $verified = false;
    foreach (apple_keys() as $key) {
        if (($key['kid'] ?? '') !== $kid || ($key['kty'] ?? '') !== 'RSA' || !isset($key['n'],$key['e'])) continue;
        $n = apple_b64((string)$key['n']); $e = apple_b64((string)$key['e']);
        if ($n === false || $e === false) continue;
        $public = openssl_pkey_get_public(apple_rsa_pem($n, $e));
        if ($public && openssl_verify("$head64.$body64", $signature, $public, OPENSSL_ALGO_SHA256) === 1) $verified = true;
        break;
    }
    if (!$verified) fail('La firma d’Apple no és vàlida', 401);
    $aud = (string)(getenv('AGENDA_APPLE_IOS_BUNDLE_ID') ?: 'com.agendavalenciana.app.ios');
    $now = time();
    if (!hash_equals('https://appleid.apple.com', (string)($claims['iss'] ?? '')) ||
        !hash_equals($aud, (string)($claims['aud'] ?? '')) ||
        (int)($claims['exp'] ?? 0) < $now ||
        (int)($claims['iat'] ?? 0) > $now + 90 || (int)($claims['iat'] ?? 0) < $now - 600 ||
        !hash_equals(hash('sha256', $nonce), (string)($claims['nonce'] ?? '')) ||
        empty($claims['sub'])) fail('Dades de verificació d’Apple incorrectes', 401);
    return $claims;
}
$data = request_data();
$jwt = (string)($data['id_token'] ?? '');
$nonce = (string)($data['nonce'] ?? '');
if (strlen($jwt) > 12000 || strlen($jwt) < 100 || strlen($nonce) < 16 || strlen($nonce) > 256) fail('Credencial invàlida', 422);
$claims = apple_verify_jwt($jwt, $nonce);
$sub = (string)$claims['sub'];
$email = filter_var($claims['email'] ?? '', FILTER_VALIDATE_EMAIL) ? strtolower((string)$claims['email']) : null;
$pdo = db();
$pdo->beginTransaction();
try {
    // A successfully used ID token cannot be replayed to mint another session.
    $pdo->prepare('INSERT INTO ios_social_token_uses(token_hash, provider) VALUES(?,?)')
        ->execute([hash('sha256', $jwt), 'APPLE']);
    $q = $pdo->prepare('SELECT u.id,u.username,u.email,u.role,u.active FROM user_apple_accounts a JOIN users u ON u.id=a.user_id WHERE a.apple_sub=? LIMIT 1 FOR UPDATE');
    $q->execute([$sub]); $user = $q->fetch(PDO::FETCH_ASSOC);
    if (!$user) {
        if ($email) {
            $q = $pdo->prepare('SELECT id FROM users WHERE LOWER(email)=LOWER(?) LIMIT 1');
            $q->execute([$email]);
            if ($q->fetchColumn()) fail('Este correu ja està registrat. Vincula Apple des del compte existent.', 409, 'ACCOUNT_EXISTS');
        }
        $username = 'apple_' . substr(hash('sha256', $sub . bin2hex(random_bytes(5))), 0, 20);
        $pdo->prepare("INSERT INTO users(username,email,password_hash,role,active,created_at,updated_at) VALUES(?,?,NULL,'PUBLIC',1,UTC_TIMESTAMP(),UTC_TIMESTAMP())")
            ->execute([$username, $email]);
        $userId = (int)$pdo->lastInsertId();
        $pdo->prepare('INSERT INTO user_apple_accounts(user_id,apple_sub) VALUES(?,?)')->execute([$userId,$sub]);
        $pdo->prepare('INSERT INTO user_profiles(user_id,display_name,bio,created_at,updated_at) VALUES(?,?,?,UTC_TIMESTAMP(),UTC_TIMESTAMP())')
            ->execute([$userId,$username,'']);
        $user = ['id'=>$userId,'username'=>$username,'email'=>$email,'role'=>'PUBLIC','active'=>1];
    }
    if (!(int)$user['active']) fail('Compte desactivat', 403);
    $token = bin2hex(random_bytes(32));
    $ttl = max(1,min(90,(int)(app_config()['token_ttl_days'] ?? 30)));
    $expires = (new DateTimeImmutable('now',new DateTimeZone('UTC')))->modify("+$ttl days")->format('Y-m-d H:i:s');
    $pdo->prepare('INSERT INTO api_tokens(user_id,token_hash,device_name,expires_at) VALUES(?,?,?,?)')
        ->execute([(int)$user['id'],hash('sha256',$token),'iOS Apple',$expires]);
    $pdo->commit();
    send_json(['ok'=>true,'token'=>$token,'user'=>[
        'id'=>(int)$user['id'],'username'=>$user['username'],'email'=>$user['email'],'role'=>$user['role']]]);
} catch (PDOException $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    if ($e->getCode() === '23000') fail('Esta credencial ja s’ha utilitzat', 409, 'TOKEN_REPLAY');
    fail('No es pot completar l’accés amb Apple', 503);
} catch (Throwable $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    throw $e;
}
