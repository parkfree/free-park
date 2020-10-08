ALTER TABLE `tenants`
    ADD COLUMN total_paid_amount INTEGER NOT NULL DEFAULT 0 AFTER email;
