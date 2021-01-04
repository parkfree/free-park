ALTER TABLE `members` ADD COLUMN `coupons` INT(20) NOT NULL DEFAULT '0' AFTsER `points`;
ALTER TABLE `payments` ADD COLUMN `qr_code` VARCHAR(50) NOT NULL AFTER `status`;
ALTER TABLE `payments` ADD COLUMN `face_price` INT(20) NOT NULL AFTER `qr_code`;