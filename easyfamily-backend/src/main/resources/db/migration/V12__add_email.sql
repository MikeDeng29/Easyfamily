-- Add optional email address to the users table (referred to as user_profile in
-- the product domain).  NULL means the user has not provided an email address.
ALTER TABLE users    ADD COLUMN email VARCHAR(200) DEFAULT NULL AFTER nickname;

-- Add email to feedback so that reply notifications can be sent to the address
-- the user provided at submission time (falls back to their profile email).
ALTER TABLE feedback ADD COLUMN email VARCHAR(200) DEFAULT NULL AFTER phone;
