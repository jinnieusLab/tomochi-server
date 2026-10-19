-- 친구 관계 하나 = 행 하나. 작은 id를 user_low, 큰 id를 user_high에 둬요.
-- A가 B의 코드를, B가 A의 코드를 동시에 넣어도 (user_low, user_high) 유니크 제약으로 관계는 하나뿐이에요.
CREATE TABLE friendship
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_low   BIGINT      NOT NULL,
    user_high  BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_friendship_pair UNIQUE (user_low, user_high),
    INDEX idx_friendship_user_high (user_high),
    CONSTRAINT fk_friendship_user_low FOREIGN KEY (user_low) REFERENCES users (id),
    CONSTRAINT fk_friendship_user_high FOREIGN KEY (user_high) REFERENCES users (id),
    CONSTRAINT ck_friendship_order CHECK (user_low < user_high)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
