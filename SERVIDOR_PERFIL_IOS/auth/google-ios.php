<?php
declare(strict_types=1);
require_once __DIR__ . '/../_bootstrap.php';

header('Cache-Control: no-store');

function google_fail(string $message, int $status = 400, string $code = 'GOOGLE_AUTH_ERROR'): never {
    if (function_exists('fail')) fail($message, $status, $code);
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['ok' => false, 'message' => $message, 'code' => $code], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

/** iOS tokens have a different audience from the website's Google tokens. */
function google_web_client_id(): string {
    $ios = trim((string)(getenv('AGENDA_GOOGLE_IOS_CLIENT_ID') ?: ''));
    if ($ios === '') google_fail('Falta configurar el client Google iOS al servidor.', 503, 'GOOGLE_IOS_NOT_CONFIGURED');
    return $ios;
}

function b64url_decode_strict(string $value): string|false {
    $value = strtr($value, '-_', '+/');
    $pad = strlen($value) % 4;
    if ($pad) $value .= str_repeat('=', 4 - $pad);
    return base64_decode($value, true);
}

function google_fetch_pem_certs(): array {
    $cache = sys_get_temp_dir() . '/agenda_google_oauth_certs.json';
    if (is_file($cache) && (time() - (int)@filemtime($cache)) < 21600) {
        $cached = json_decode((string)@file_get_contents($cache), true);
        if (is_array($cached) && $cached) return $cached;
    }

    $url = 'https://www.googleapis.com/oauth2/v1/certs';
    $body = false;
    if (function_exists('curl_init')) {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_CONNECTTIMEOUT => 5,
            CURLOPT_TIMEOUT => 10,
            CURLOPT_SSL_VERIFYPEER => true,
            CURLOPT_HTTPHEADER => ['Accept: application/json'],
        ]);
        $body = curl_exec($ch);
        $status = (int)curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
        curl_close($ch);
        if ($status !== 200) $body = false;
    } elseif (ini_get('allow_url_fopen')) {
        $ctx = stream_context_create(['http' => ['timeout' => 10, 'header' => "Accept: application/json\r\n"]]);
        $body = @file_get_contents($url, false, $ctx);
    }
    if (!is_string($body) || $body === '') google_fail('No s’han pogut validar les claus de Google.', 503, 'GOOGLE_KEYS_UNAVAILABLE');
    $certs = json_decode($body, true);
    if (!is_array($certs) || !$certs) google_fail('Resposta de claus de Google no vàlida.', 503, 'GOOGLE_KEYS_INVALID');
    @file_put_contents($cache, json_encode($certs, JSON_UNESCAPED_SLASHES), LOCK_EX);
    return $certs;
}

function verify_google_id_token(string $jwt, string $expectedAudience, string $expectedNonce): array {
    $parts = explode('.', $jwt);
    if (count($parts) !== 3) google_fail('Token de Google no vàlid.', 401, 'INVALID_GOOGLE_TOKEN');
    [$h64, $p64, $s64] = $parts;
    $headerJson = b64url_decode_strict($h64);
    $payloadJson = b64url_decode_strict($p64);
    $signature = b64url_decode_strict($s64);
    if ($headerJson === false || $payloadJson === false || $signature === false) google_fail('Token de Google mal format.', 401, 'INVALID_GOOGLE_TOKEN');
    $header = json_decode($headerJson, true);
    $payload = json_decode($payloadJson, true);
    if (!is_array($header) || !is_array($payload)) google_fail('Token de Google mal format.', 401, 'INVALID_GOOGLE_TOKEN');
    if (($header['alg'] ?? '') !== 'RS256') google_fail('Algoritme de token de Google no admés.', 401, 'INVALID_GOOGLE_ALG');
    $kid = trim((string)($header['kid'] ?? ''));
    if ($kid === '') google_fail('Token de Google sense clau de signatura.', 401, 'INVALID_GOOGLE_TOKEN');

    $certs = google_fetch_pem_certs();
    $pem = $certs[$kid] ?? null;
    if (!is_string($pem) || $pem === '') {
        // Força renovació de la cache una volta per si Google acaba de rotar les claus.
        @unlink(sys_get_temp_dir() . '/agenda_google_oauth_certs.json');
        $certs = google_fetch_pem_certs();
        $pem = $certs[$kid] ?? null;
    }
    if (!is_string($pem) || $pem === '') google_fail('Clau de signatura de Google desconeguda.', 401, 'UNKNOWN_GOOGLE_KEY');
    $ok = openssl_verify($h64 . '.' . $p64, $signature, $pem, OPENSSL_ALGO_SHA256);
    if ($ok !== 1) google_fail('La signatura del token de Google no és vàlida.', 401, 'INVALID_GOOGLE_SIGNATURE');

    $iss = (string)($payload['iss'] ?? '');
    if ($iss !== 'accounts.google.com' && $iss !== 'https://accounts.google.com') google_fail('Emissor de Google no vàlid.', 401, 'INVALID_GOOGLE_ISSUER');
    if (!hash_equals($expectedAudience, (string)($payload['aud'] ?? ''))) google_fail('El token de Google pertany a una altra aplicació.', 401, 'INVALID_GOOGLE_AUDIENCE');
    $now = time();
    if ((int)($payload['exp'] ?? 0) <= $now - 30) google_fail('El token de Google ha caducat.', 401, 'GOOGLE_TOKEN_EXPIRED');
    if ((int)($payload['iat'] ?? 0) > $now + 120) google_fail('Data del token de Google no vàlida.', 401, 'INVALID_GOOGLE_TOKEN_TIME');
    if ($expectedNonce === '' || !isset($payload['nonce']) || !hash_equals($expectedNonce, (string)$payload['nonce'])) google_fail('La verificació de seguretat de Google no coincidix.', 401, 'INVALID_GOOGLE_NONCE');
    if (empty($payload['sub'])) google_fail('Compte de Google sense identificador.', 401, 'INVALID_GOOGLE_SUBJECT');
    if (empty($payload['email']) || filter_var($payload['email'], FILTER_VALIDATE_EMAIL) === false) google_fail('Google no ha proporcionat un correu vàlid.', 401, 'INVALID_GOOGLE_EMAIL');
    if (!filter_var($payload['email_verified'] ?? false, FILTER_VALIDATE_BOOLEAN)) google_fail('El correu de Google no està verificat.', 401, 'GOOGLE_EMAIL_NOT_VERIFIED');
    return $payload;
}

function google_ensure_table(PDO $pdo): void {
    $pdo->exec("CREATE TABLE IF NOT EXISTS user_google_accounts (
        user_id BIGINT(20) UNSIGNED NOT NULL,
        google_sub VARCHAR(255) NOT NULL,
        google_email VARCHAR(190) NOT NULL,
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (user_id),
        UNIQUE KEY uq_google_accounts_sub (google_sub),
        KEY idx_google_accounts_email (google_email),
        CONSTRAINT fk_google_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
}

function table_exists(PDO $pdo, string $table): bool {
    $q = $pdo->prepare("SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? LIMIT 1");
    $q->execute([$table]);
    return (bool)$q->fetchColumn();
}

function column_exists(PDO $pdo, string $table, string $column): bool {
    $q = $pdo->prepare("SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=? LIMIT 1");
    $q->execute([$table, $column]);
    return (bool)$q->fetchColumn();
}

function unique_google_username(PDO $pdo, string $email, string $name): string {
    $seed = trim($name) !== '' ? $name : explode('@', $email, 2)[0];
    $seed = iconv('UTF-8', 'ASCII//TRANSLIT//IGNORE', $seed) ?: $seed;
    $seed = preg_replace('/[^A-Za-z0-9._-]+/', '', $seed) ?: '';
    $seed = trim($seed, '._-');
    if ($seed === '') $seed = 'usuari';
    $seed = mb_substr($seed, 0, 64, 'UTF-8');
    $candidate = $seed;
    $n = 2;
    $q = $pdo->prepare('SELECT 1 FROM users WHERE LOWER(username)=LOWER(?) LIMIT 1');
    while (true) {
        $q->execute([$candidate]);
        if (!$q->fetchColumn()) return $candidate;
        $suffix = (string)$n++;
        $candidate = mb_substr($seed, 0, max(1, 78 - strlen($suffix)), 'UTF-8') . $suffix;
        if ($n > 1000) $candidate = 'usuari' . bin2hex(random_bytes(4));
    }
}

$data = function_exists('request_data') ? request_data() : json_decode((string)file_get_contents('php://input'), true);
if (!is_array($data)) $data = [];
$idToken = trim((string)($data['id_token'] ?? ''));
$nonce = trim((string)($data['nonce'] ?? ''));
$deviceName = trim((string)($data['device_name'] ?? 'Android')) ?: 'Android';
if ($idToken === '' || $nonce === '') google_fail('Falten dades d’accés amb Google.', 422, 'GOOGLE_DATA_REQUIRED');

$audience = google_web_client_id();
$google = verify_google_id_token($idToken, $audience, $nonce);
$sub = (string)$google['sub'];
$email = mb_strtolower(trim((string)$google['email']), 'UTF-8');
$name = trim((string)($google['name'] ?? ''));
$picture = trim((string)($google['picture'] ?? ''));
$hd = trim((string)($google['hd'] ?? ''));
$domain = mb_strtolower((string)substr(strrchr($email, '@') ?: '', 1), 'UTF-8');
$googleAuthoritative = $domain === 'gmail.com' || $hd !== '';

$pdo = db();
google_ensure_table($pdo);

$hasEmailVerifiedAt = column_exists($pdo, 'users', 'email_verified_at');
$hasProfiles = table_exists($pdo, 'user_profiles');

$pdo->beginTransaction();
try {
    $pdo->prepare('INSERT INTO ios_social_token_uses(token_hash, provider) VALUES(?,?)')
        ->execute([hash('sha256', $idToken), 'GOOGLE']);
    $stmt = $pdo->prepare('SELECT u.id,u.username,u.email,u.role,u.active FROM user_google_accounts g JOIN users u ON u.id=g.user_id WHERE g.google_sub=? LIMIT 1 FOR UPDATE');
    $stmt->execute([$sub]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC) ?: null;

    if (!$user) {
        $fields = 'id,username,email,role,active';
        if ($hasEmailVerifiedAt) $fields .= ',email_verified_at';
        $stmt = $pdo->prepare("SELECT $fields FROM users WHERE LOWER(email)=LOWER(?) LIMIT 1 FOR UPDATE");
        $stmt->execute([$email]);
        $existing = $stmt->fetch(PDO::FETCH_ASSOC) ?: null;

        if ($existing) {
            $alreadyVerified = $hasEmailVerifiedAt && !empty($existing['email_verified_at']);
            if (!$googleAuthoritative && !$alreadyVerified) {
                $pdo->rollBack();
                google_fail('Este correu ja té un compte. Inicia sessió amb la contrasenya abans de vincular Google.', 409, 'GOOGLE_LINK_REQUIRED');
            }
            $user = $existing;
            $link = $pdo->prepare('INSERT INTO user_google_accounts(user_id,google_sub,google_email) VALUES(?,?,?)');
            $link->execute([(int)$user['id'], $sub, $email]);
            if ($hasEmailVerifiedAt && $googleAuthoritative && empty($user['email_verified_at'])) {
                $pdo->prepare('UPDATE users SET email_verified_at=COALESCE(email_verified_at,NOW()) WHERE id=?')->execute([(int)$user['id']]);
            }
        } else {
            $username = unique_google_username($pdo, $email, $name);
            if ($hasEmailVerifiedAt && $googleAuthoritative) {
                $ins = $pdo->prepare("INSERT INTO users(username,email,email_verified_at,password_hash,role,active) VALUES(?,?,NOW(),NULL,'PUBLIC',1)");
                $ins->execute([$username, $email]);
            } else {
                $ins = $pdo->prepare("INSERT INTO users(username,email,password_hash,role,active) VALUES(?,?,NULL,'PUBLIC',1)");
                $ins->execute([$username, $email]);
            }
            $userId = (int)$pdo->lastInsertId();
            $pdo->prepare('INSERT INTO user_google_accounts(user_id,google_sub,google_email) VALUES(?,?,?)')->execute([$userId, $sub, $email]);
            if ($hasProfiles) {
                $display = $name !== '' ? mb_substr($name, 0, 190, 'UTF-8') : $username;
                $photo = $picture !== '' ? mb_substr($picture, 0, 500, 'UTF-8') : null;
                $pdo->prepare('INSERT INTO user_profiles(user_id,display_name,bio,photo_url) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE display_name=COALESCE(NULLIF(display_name,\'\'),VALUES(display_name)), photo_url=COALESCE(photo_url,VALUES(photo_url))')
                    ->execute([$userId, $display, '', $photo]);
            }
            $user = ['id'=>$userId,'username'=>$username,'email'=>$email,'role'=>'PUBLIC','active'=>1];
        }
    }

    if ((int)($user['active'] ?? 0) !== 1) {
        $pdo->rollBack();
        google_fail('Este compte està desactivat.', 403, 'ACCOUNT_DISABLED');
    }

    $rawToken = bin2hex(random_bytes(32));
    $tokenHash = hash('sha256', $rawToken);
    $expires = (new DateTimeImmutable('now', new DateTimeZone('UTC')))->modify('+30 days')->format('Y-m-d H:i:s');
    $insertToken = $pdo->prepare('INSERT INTO api_tokens(user_id,token_hash,device_name,expires_at) VALUES(?,?,?,?)');
    $insertToken->execute([(int)$user['id'], $tokenHash, mb_substr($deviceName,0,190,'UTF-8'), $expires]);

    $displayName = (string)$user['username'];
    if ($hasProfiles) {
        $q = $pdo->prepare('SELECT display_name FROM user_profiles WHERE user_id=? LIMIT 1');
        $q->execute([(int)$user['id']]);
        $profileName = trim((string)($q->fetchColumn() ?: ''));
        if ($profileName !== '') $displayName = $profileName;
    }
    $pdo->commit();

    if (function_exists('json_response')) {
        json_response(['ok'=>true,'token'=>$rawToken,'user'=>[
            'id'=>(int)$user['id'],'username'=>(string)$user['username'],'display_name'=>$displayName,
            'email'=>$email,'role'=>(string)($user['role'] ?? 'PUBLIC')
        ]]);
        exit;
    }
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['ok'=>true,'token'=>$rawToken,'user'=>[
        'id'=>(int)$user['id'],'username'=>(string)$user['username'],'display_name'=>$displayName,
        'email'=>$email,'role'=>(string)($user['role'] ?? 'PUBLIC')
    ]], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
} catch (PDOException $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    if ($e->getCode() === '23000') google_fail('Credencial ja utilitzada o compte no vinculable.', 409, 'TOKEN_REPLAY_OR_ACCOUNT_CONFLICT');
    google_fail('No s’ha pogut completar l’accés amb Google.', 503, 'GOOGLE_DATABASE_ERROR');
} catch (Throwable $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    google_fail('No s’ha pogut completar l’accés amb Google.', 500, 'GOOGLE_LOGIN_FAILED');
}
