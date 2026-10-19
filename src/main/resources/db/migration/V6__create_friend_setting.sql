-- 한쪽에서만 보이는 친구 설정. owner가 friend를 목록에서 숨겼는지만 둬요. 상대는 몰라요.
CREATE TABLE friend_setting
(
    owner_id   BIGINT      NOT NULL,
    friend_id  BIGINT      NOT NULL,
    hidden     BOOLEAN     NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (owner_id, friend_id),
    CONSTRAINT fk_friend_setting_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_friend_setting_friend FOREIGN KEY (friend_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
