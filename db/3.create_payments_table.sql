CREATE TABLE IF NOT EXISTS `payments`
(
    `id`         INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `tenant_id`  INTEGER             NOT NULL,
    `member_id`  INTEGER,
    `amount`     INTEGER             NOT NULL DEFAULT 0,
    `status`     VARCHAR(30)         NOT NULL,
    `comment`    TEXT                NOT NULL,
    `paid_at`    TIMESTAMP           NOT NULL,
    `updated_at` TIMESTAMP                    DEFAULT CURRENT_TIMESTAMP,
    `created_at` TIMESTAMP                    DEFAULT CURRENT_TIMESTAMP,
    INDEX `tenant_id` (`tenant_id`),
    INDEX `member_id` (`member_id`),
    INDEX `paid_at` (`paid_at`)
);
