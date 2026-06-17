CREATE TABLE family_asset (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      VARCHAR(64)    NOT NULL,
    name         VARCHAR(100)   NOT NULL,
    asset_type   VARCHAR(20)    NOT NULL,
    asset_value  DECIMAL(15,2)  NOT NULL,
    note         VARCHAR(500),
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_asset_user ON family_asset (user_id);

CREATE TABLE family_liability (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         VARCHAR(64)    NOT NULL,
    name            VARCHAR(100)   NOT NULL,
    liability_type  VARCHAR(20)    NOT NULL,
    balance         DECIMAL(15,2)  NOT NULL,
    monthly_payment DECIMAL(15,2),
    interest_rate   DECIMAL(5,2),
    note            VARCHAR(500),
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_liability_user ON family_liability (user_id);
