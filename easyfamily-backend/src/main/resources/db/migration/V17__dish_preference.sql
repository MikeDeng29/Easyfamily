CREATE TABLE IF NOT EXISTS dish_preferences (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id    VARCHAR(64)  NOT NULL,
    dish_name  VARCHAR(200) NOT NULL,
    weight     INT          NOT NULL DEFAULT 1,
    updated_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dish_pref (user_id, dish_name),
    INDEX idx_dish_pref_user_id (user_id)
);
