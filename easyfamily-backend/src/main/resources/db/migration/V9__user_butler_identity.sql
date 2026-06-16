-- Add optional AI butler identity customization columns to the users table.
-- Each user can rename their AI butler, pick an avatar (1..8), and pick a
-- persona/tone (warm/strict/humorous). NULL values mean "use default" and
-- are resolved by the application layer (UserProfileService) -- defaults are
-- not enforced via SQL DEFAULT to keep "unset" distinguishable if needed.
--
-- As with V7 (nickname), SchemaBootstrapService's CREATE TABLE IF NOT EXISTS
-- for `users` may run before or after this migration, so it has been updated
-- to include these columns for brand-new databases, while the ALTER TABLE
-- statements below handle pre-existing `users` tables.

ALTER TABLE users ADD COLUMN IF NOT EXISTS butler_name VARCHAR(10) NULL AFTER nickname;
ALTER TABLE users ADD COLUMN IF NOT EXISTS butler_avatar_id INT NULL AFTER butler_name;
ALTER TABLE users ADD COLUMN IF NOT EXISTS butler_persona VARCHAR(16) NULL AFTER butler_avatar_id;
