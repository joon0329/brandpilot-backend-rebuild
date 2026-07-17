

CREATE TABLE users (
    user_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    refresh_token_hash VARCHAR(64) NULL,
    refresh_token_expires_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_refresh_token_hash UNIQUE (refresh_token_hash),
    CONSTRAINT chk_users_refresh_token_pair CHECK (
        (refresh_token_hash IS NULL AND refresh_token_expires_at IS NULL)
        OR
        (refresh_token_hash IS NOT NULL AND refresh_token_expires_at IS NOT NULL)
    )
);
