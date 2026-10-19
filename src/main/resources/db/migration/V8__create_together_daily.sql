-- owner가 자기 달력(시간대)으로 보는 friend와의 하루 겹친 시간. 같은 겹침이 두 사람 쪽으로 한 줄씩 들어가요.
CREATE TABLE together_daily
(
    owner_id   BIGINT      NOT NULL,
    friend_id  BIGINT      NOT NULL,
    date       DATE        NOT NULL,
    seconds    INT         NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (owner_id, friend_id, date),
    INDEX idx_together_daily_owner_date (owner_id, date),
    CONSTRAINT fk_together_daily_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_together_daily_friend FOREIGN KEY (friend_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
