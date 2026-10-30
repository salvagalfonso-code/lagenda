-- Execute AFTER 01_for_you_server_cache.sql, on the same MariaDB used by Android.
CREATE TABLE IF NOT EXISTS user_for_you_preferences (
  user_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
  suggestions TINYINT(1) NOT NULL DEFAULT 1,
  discovery ENUM('MY_TASTES','BALANCED','EXPLORE') NOT NULL DEFAULT 'BALANCED',
  valencian_first TINYINT(1) NOT NULL DEFAULT 1,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_user_for_you_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
