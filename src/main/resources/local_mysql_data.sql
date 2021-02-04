INSERT INTO tenants (id, car_number, email, owner, password, role, total_paid_amount) VALUES (1, '川A00000', 'test@qq.com', 'sa', '$2a$10$U93kNLcGQGmomDvX45dIeebYFJWP5mWVDwKyT1FHiU0SVd9Eys6MK', 'ROLE_USER', 0)


INSERT INTO members (tenant_id, user_id, open_id, coupons, mem_type, mobile, enable_pay, enable_point, points) VALUES (1, 'user11111', 'openid11111', 8, 'in卡', '13000000000', true, true, 0)

