-- Step 30: idempotent APNs notification delivery for existing server inbox messages.
-- Run only on agendapv, after earlier APNs migration. No Android tables modified.
CREATE TABLE IF NOT EXISTS ios_apns_inbox_delivery (
 inbox_id BIGINT UNSIGNED NOT NULL,
 device_token CHAR(128) NOT NULL,
 attempts SMALLINT UNSIGNED NOT NULL DEFAULT 0,
 accepted_at DATETIME NULL,
 retry_after DATETIME NULL,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(inbox_id,device_token),
 KEY idx_retry (accepted_at,retry_after)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
