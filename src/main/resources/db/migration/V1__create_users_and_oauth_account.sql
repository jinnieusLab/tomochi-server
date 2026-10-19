CREATE TABLE users
(
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    nickname     VARCHAR(12) NULL,
    pet_kind     VARCHAR(20) NOT NULL,
    status_icon  VARCHAR(20) NULL,
    timezone     VARCHAR(64) NOT NULL,
    invite_code  CHAR(6) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    last_seen_at DATETIME(6) NULL,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    deleted_at   DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_invite_code UNIQUE (invite_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 같은 이메일이라도 제공자가 다르면 다른 계정이에요. 이메일은 저장하지 않아요.
CREATE TABLE oauth_account
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    provider         VARCHAR(20)  NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_oauth_account_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT fk_oauth_account_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
