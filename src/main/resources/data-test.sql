-- ！！！请勿在生产环境中使用！！！
INSERT INTO auth_users (id, username, email, phone, password, created_at)
VALUES (303066393459429376, 'adminuser', 'admin@test.com', '13000000000', '$2a$10$tfHHW2.0NwLIPgj0HG9YduJh8vUH.RnMx7Y0HJntNqqP4hCV5ZsnG',
        TIMESTAMP '2021-01-01 15:00:00') ON CONFLICT (id) DO NOTHING;
-- password: admin123
INSERT INTO user_role (user_id, role_id) VALUES (303066393459429376, 1) ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO auth_users (id, username, email, phone, password, created_at)
VALUES (303066491119603712, 'testuser', 'test@test.com', '13012345678', '$2a$10$Le4d33m0uXF0xoXRKAiuWOEtxnDybcFAMwCBcdLUeZtY8ogaslLwS',
        TIMESTAMP '2022-01-01 15:00:00') ON CONFLICT (id) DO NOTHING;
-- password: abc123456789
INSERT INTO user_role (user_id, role_id) VALUES (303066491119603712, 2) ON CONFLICT (user_id, role_id) DO NOTHING;
