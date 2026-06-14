CREATE TABLE bill (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    VARCHAR(64)    NOT NULL,
    category   VARCHAR(50)    NOT NULL,
    amount     DECIMAL(10, 2) NOT NULL,
    note       VARCHAR(200),
    billed_at  DATE           NOT NULL,
    created_at TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_bill_user ON bill (user_id);
CREATE INDEX idx_bill_user_month ON bill (user_id, billed_at);
