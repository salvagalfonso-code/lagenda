-- L'AGENDA · iOS artist bell alerts (MariaDB/InnoDB/utf8mb4).
-- Apply once to the EXISTING agendapv database. No existing tables are dropped.
CREATE TABLE IF NOT EXISTS ios_artist_bells (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  artist_key VARCHAR(190) COLLATE utf8mb4_unicode_ci NOT NULL,
  artist_name VARCHAR(190) NOT NULL,
  frequency ENUM('IMMEDIATE','DAILY','WEEKLY') NOT NULL DEFAULT 'WEEKLY',
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  activated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_ibs_user_artist (user_id,artist_key),
  KEY idx_ibs_enabled (enabled,frequency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ios_apns_devices (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  device_token CHAR(128) NOT NULL,
  environment ENUM('development','production') NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  last_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_iad_token (device_token),
  KEY idx_iad_user (user_id,enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ios_artist_alert_pending (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  artist_key VARCHAR(190) COLLATE utf8mb4_unicode_ci NOT NULL,
  event_id BIGINT UNSIGNED NOT NULL,
  discovered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  delivered_at DATETIME DEFAULT NULL,
  read_at DATETIME DEFAULT NULL,
  attempts INT UNSIGNED NOT NULL DEFAULT 0,
  retry_after DATETIME DEFAULT NULL,
  UNIQUE KEY uq_iap_event (user_id,artist_key,event_id),
  KEY idx_iap_queue (delivered_at,retry_after,discovered_at),
  KEY idx_iap_user_artist (user_id,artist_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
