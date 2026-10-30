-- Aplicar després d'una còpia de seguretat. No modifica la taula users ni les vendes.
CREATE TABLE IF NOT EXISTS user_apple_accounts (
  user_id BIGINT UNSIGNED NOT NULL,
  apple_sub VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  UNIQUE KEY uq_user_apple_sub (apple_sub),
  CONSTRAINT fk_user_apple_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS ios_social_token_uses (
  token_hash CHAR(64) NOT NULL PRIMARY KEY,
  provider VARCHAR(12) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_ios_social_token_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- Purga segura opcional: DELETE FROM ios_social_token_uses WHERE created_at < UTC_TIMESTAMP() - INTERVAL 24 HOUR;
