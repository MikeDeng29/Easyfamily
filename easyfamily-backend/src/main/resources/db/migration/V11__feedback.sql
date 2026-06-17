CREATE TABLE feedback (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     VARCHAR(64)  NOT NULL,
    phone       VARCHAR(20)  NOT NULL,
    title       VARCHAR(200),
    description TEXT         NOT NULL,
    status      ENUM('pending','replied','closed') NOT NULL DEFAULT 'pending',
    reply       TEXT,
    replied_at  DATETIME,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_feedback_user    (user_id),
    INDEX idx_feedback_status  (status),
    INDEX idx_feedback_created (created_at)
);
