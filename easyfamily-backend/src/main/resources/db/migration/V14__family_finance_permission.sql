CREATE TABLE family_finance_permission (
    head_user_id  VARCHAR(64)  NOT NULL,
    viewer_phone  VARCHAR(20)  NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (head_user_id, viewer_phone)
);
