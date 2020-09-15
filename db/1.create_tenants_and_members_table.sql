CREATE TABLE IF NOT EXISTS `members`
(
    `id`           INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `tenant_id`    INT(11),
    `user_id`      VARCHAR(32)         NOT NULL DEFAULT '',
    `open_id`      VARCHAR(32)         NOT NULL DEFAULT '',
    `mem_type`     VARCHAR(20)         NOT NULL DEFAULT '',
    `mobile`       VARCHAR(20)         NOT NULL DEFAULT '',
    `last_paid_at` DATE                NOT NULL DEFAULT '2020-01-01',
    `created_at`   TIMESTAMP                    DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   TIMESTAMP                    DEFAULT CURRENT_TIMESTAMP,
    INDEX `tenant_id` (`tenant_id`),
    INDEX `last_paid_at` (`last_paid_at`)
);

CREATE TABLE IF NOT EXISTS `tenants`
(
    `id`         INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `car_number` VARCHAR(20)         NOT NULL DEFAULT '',
    `owner`      VARCHAR(32)         NOT NULL DEFAULT '',
    `role`       VARCHAR(15)         NOT NULL DEFAULT 'ROLE_USER',
    `created_at` TIMESTAMP                    DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP                    DEFAULT CURRENT_TIMESTAMP
);
