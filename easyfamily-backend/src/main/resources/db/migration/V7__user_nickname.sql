-- Add an optional nickname column to the users table so that the nickname
-- chosen on first use of the iOS chat onboarding flow can be persisted
-- server-side (previously stored only in client-side UserDefaults) and
-- survives device changes / reinstalls.
--
-- The `users` table itself is created by SchemaBootstrapService at
-- application startup (CREATE TABLE IF NOT EXISTS), which may run before or
-- after this migration depending on bean initialization order. The
-- CREATE TABLE below is therefore a no-op safety net that mirrors the
-- bootstrap schema (including nickname) for brand-new databases, while the
-- ALTER TABLE handles the case where `users` already existed without the
-- column.

CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL UNIQUE,
  phone VARCHAR(32) NOT NULL,
  nickname VARCHAR(64),
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_users_phone (phone)
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS nickname VARCHAR(64) NULL AFTER phone;
