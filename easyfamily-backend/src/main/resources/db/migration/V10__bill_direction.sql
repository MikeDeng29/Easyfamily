ALTER TABLE bill
    ADD COLUMN direction ENUM('income', 'expense') NOT NULL DEFAULT 'expense';

CREATE INDEX idx_bill_user_direction ON bill (user_id, direction);
