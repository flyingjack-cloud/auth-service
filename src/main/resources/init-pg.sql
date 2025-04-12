-- Postgresql初始化脚本
-- create necessary data types
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'citext') THEN
            CREATE EXTENSION citext;
        END IF;
        -- phone number
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'cn_phone_number') THEN
            CREATE DOMAIN cn_phone_number AS varchar(11) CHECK ( value ~ '^1[3-9]\d{9}$');
        END IF;
        -- email
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'email') THEN
            CREATE DOMAIN email AS citext
                CHECK ( value ~
                        '^[a-zA-Z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$' );
        END IF;
    END
$$;