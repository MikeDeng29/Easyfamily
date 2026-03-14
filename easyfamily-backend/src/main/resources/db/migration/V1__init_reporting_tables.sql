CREATE TABLE IF NOT EXISTS report_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  event_time DATETIME(3) NOT NULL,
  event_date DATE NOT NULL,
  event_name VARCHAR(64) NOT NULL,
  event_version VARCHAR(16) NOT NULL,
  user_id VARCHAR(64),
  login_phone VARCHAR(20),
  target_phone VARCHAR(20),
  session_id VARCHAR(64),
  request_id VARCHAR(64),
  platform VARCHAR(32),
  client_type VARCHAR(16),
  app_version VARCHAR(32),
  ip VARCHAR(64),
  query_type VARCHAR(32),
  provider_key VARCHAR(32),
  cache_hit TINYINT(1),
  status_code VARCHAR(32),
  latency_ms INT,
  ext_json JSON,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  INDEX idx_event_date_name (event_date, event_name),
  INDEX idx_event_date_user (event_date, user_id),
  INDEX idx_event_time (event_time),
  INDEX idx_request_id (request_id)
);

CREATE TABLE IF NOT EXISTS report_metric_daily (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  metric_date DATE NOT NULL,
  metric_name VARCHAR(64) NOT NULL,
  dim1 VARCHAR(64) NOT NULL DEFAULT '',
  dim2 VARCHAR(64) NOT NULL DEFAULT '',
  dim3 VARCHAR(64) NOT NULL DEFAULT '',
  metric_value BIGINT,
  metric_value_decimal DECIMAL(18,6),
  calculated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  source VARCHAR(32) NOT NULL DEFAULT 'event-aggregation',
  UNIQUE KEY uk_metric (metric_date, metric_name, dim1, dim2, dim3),
  INDEX idx_metric_date_name (metric_date, metric_name),
  INDEX idx_metric_name_dim1 (metric_name, dim1)
);

CREATE TABLE IF NOT EXISTS report_task_run (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_name VARCHAR(64) NOT NULL,
  biz_date DATE NOT NULL,
  started_at DATETIME(3) NOT NULL,
  ended_at DATETIME(3),
  status VARCHAR(16) NOT NULL,
  error_message VARCHAR(500),
  rows_affected INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_task_date (task_name, biz_date)
);
