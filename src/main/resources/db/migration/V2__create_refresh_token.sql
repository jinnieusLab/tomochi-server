-- Refresh Token은 원문 대신 SHA-256 해시만 저장해요.
-- family_id는 한 번의 로그인(기기)에서 회전으로 이어지는 토큰 묶음이에요 (AUTH-2).
CREATE TABLE refresh_token
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    family_id    CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    token_hash   CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    device_name  VARCHAR(100) NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    last_used_at DATETIME(6)  NOT NULL,
    expires_at   DATETIME(6)  NOT NULL,
    revoked_at   DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    INDEX idx_refresh_token_family (family_id),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
