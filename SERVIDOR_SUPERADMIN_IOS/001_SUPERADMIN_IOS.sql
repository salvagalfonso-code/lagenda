-- Apply manually after backing up the database.
-- Protected against changing profile fields: access-only operations.
CREATE TABLE IF NOT EXISTS agenda_admin_access_audit (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  actor_user_id BIGINT UNSIGNED NOT NULL,
  target_user_id BIGINT UNSIGNED NOT NULL,
  previous_role VARCHAR(32) NOT NULL,
  new_role VARCHAR(32) NOT NULL,
  previous_active TINYINT(1) NOT NULL,
  new_active TINYINT(1) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_admin_access_actor (actor_user_id,created_at),
  INDEX idx_admin_access_target (target_user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
