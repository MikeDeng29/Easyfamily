CREATE TABLE IF NOT EXISTS card_monitor_snapshot (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id       VARCHAR(64) NOT NULL,
  member_id     VARCHAR(64) NOT NULL,
  member_phone  VARCHAR(20) NOT NULL,
  snapshot_json TEXT NOT NULL,
  card_count    INT NOT NULL DEFAULT 0,
  risk_level    VARCHAR(16),
  risk_summary  TEXT,
  checked_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  INDEX idx_user_checked (user_id, checked_at),
  INDEX idx_member (member_id)
);

CREATE TABLE IF NOT EXISTS device_push_token (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id    VARCHAR(64) NOT NULL,
  fcm_token  VARCHAR(256) NOT NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_user (user_id)
);
