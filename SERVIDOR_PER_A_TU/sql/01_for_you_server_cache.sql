CREATE TABLE IF NOT EXISTS for_you_server_cache (
  user_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
  signature CHAR(64) NOT NULL,
  ids_json LONGTEXT NOT NULL,
  expires_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_for_you_expiry(expires_at),
  CONSTRAINT fk_for_you_server_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
