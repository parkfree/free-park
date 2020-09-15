CREATE TABLE IF NOT EXISTS `access_tokens`
(
    `id`         INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `tenant_id`  INT(11)             NOT NULL,
    `token`      VARCHAR(70)         NOT NULL DEFAULT '',
    `expire_at`  TIMESTAMP           NOT NULL,
    `created_at` TIMESTAMP                    DEFAULT CURRENT_TIMESTAMP,
    INDEX `tenant_id` (`tenant_id`),
    UNIQUE INDEX `token` (`token`)
);

ALTER TABLE `tenants`
    ADD COLUMN password VARCHAR(70) NOT NULL DEFAULT '' AFTER email,
    ADD COLUMN role     VARCHAR(15) NOT NULL DEFAULT 'ROLE_USER' AFTER email;

ALTER TABLE `tenants`
    ADD UNIQUE INDEX `email` (`email`),
    ADD UNIQUE INDEX `car_number` (`car_number`);
