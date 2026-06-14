-- MySQL-only: enable InnoDB tablespace encryption-at-rest for tables holding
-- financial data (bill) and PII (report_event: login_phone, target_phone).
--
-- Requires the MySQL keyring plugin (e.g. keyring_file) to be installed and
-- enabled on the server (`early-plugin-load=keyring_file.so` /
-- `keyring_file_data=<path>` in my.cnf) before this migration runs, otherwise
-- the ALTER TABLE statements below will fail with ER_CANNOT_USE_ENCRYPTION_KEYRING.
--
-- This migration is intentionally kept out of the default
-- classpath:db/migration location because H2 (used by the test suite) does
-- not support the ENCRYPTION table option. It is only loaded in production
-- via the additional Flyway location configured in application-prod.yml.

ALTER TABLE bill ENCRYPTION = 'Y';

ALTER TABLE report_event ENCRYPTION = 'Y';
