-- oauth2 client注册
CREATE TABLE IF NOT EXISTS oauth2_registered_client
(
    id                            serial primary key          not null UNIQUE,
    client_id                     varchar(100)                NOT NULL,
    client_id_issued_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_secret                 varchar(200)                         DEFAULT NULL,
    client_secret_expires_at      timestamp                            DEFAULT NULL,
    client_name                   varchar(200)                NOT NULL,
    client_authentication_methods varchar(1000)               NOT NULL,
    authorization_grant_types     varchar(1000)               NOT NULL,
    redirect_uris                 varchar(1000)                        DEFAULT NULL,
    post_logout_redirect_uris     varchar(1000)                        DEFAULT NULL,
    scopes                        varchar(1000)               NOT NULL,
    client_settings               varchar(2000)               NOT NULL,
    token_settings                varchar(2000)               NOT NULL,
    -- 以下字段为拓展字段
    description                   TEXT,
    avatar_url                    VARCHAR(512),
    contact_email                 VARCHAR(255)
);

-- 用户表
CREATE TABLE IF NOT EXISTS auth_users
(
    id                      BIGINT primary key          not null UNIQUE,
    username                varchar(50)                 not null UNIQUE CHECK (length(username) >= 5),
    email                   email                       null unique,
    phone                   cn_phone_number             null unique,
    password                varchar(500)                not null CHECK ( length(password) >= 8 ),
    created_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    account_non_expired     boolean                              default true,
    account_non_locked      boolean                              default true,
    credentials_non_Expired boolean                              default true,
    enabled                 boolean                              default true
);

-- 角色表
CREATE TABLE IF NOT EXISTS auth_roles
(
    id   serial primary key not null UNIQUE,
    name VARCHAR(50)        NOT NULL UNIQUE
);

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS user_role
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES auth_users (id),
    FOREIGN KEY (role_id) REFERENCES auth_roles (id),
    UNIQUE (user_id, role_id)
);

INSERT INTO auth_roles (id, name)
VALUES (1, 'ROLE_ADMIN'),
       (2, 'ROLE_USER'),
       (3, 'ROLE_GUEST')
ON CONFLICT (id) DO NOTHING ;