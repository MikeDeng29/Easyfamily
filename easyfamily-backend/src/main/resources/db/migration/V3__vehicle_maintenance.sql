CREATE TABLE IF NOT EXISTS vehicles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  plate_number VARCHAR(16) NOT NULL,
  brand VARCHAR(32) NOT NULL,
  model VARCHAR(64) NOT NULL,
  `year` INT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vehicle_user ON vehicles (user_id);

CREATE TABLE IF NOT EXISTS maintenance_records (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  vehicle_id BIGINT NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  service_date DATE NOT NULL,
  mileage_km INT,
  shop_name VARCHAR(128),
  total_cost DECIMAL(10,2) NOT NULL DEFAULT 0,
  notes VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mr_vehicle ON maintenance_records (vehicle_id);
CREATE INDEX idx_mr_user_date ON maintenance_records (user_id, service_date);

CREATE TABLE IF NOT EXISTS maintenance_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  record_id BIGINT NOT NULL,
  category VARCHAR(32) NOT NULL,
  item_name VARCHAR(128) NOT NULL,
  cost DECIMAL(10,2) NOT NULL DEFAULT 0,
  is_diy TINYINT(1) NOT NULL DEFAULT 0,
  notes VARCHAR(200)
);

CREATE INDEX idx_mi_record ON maintenance_items (record_id);
