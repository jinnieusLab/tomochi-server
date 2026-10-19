-- 지난 날의 하루 요약 캐시. 원본은 activity_segment이고, 여기는 다시 계산할 수 있는 값만 둬요.
-- 더 이상 바뀔 수 없는 날(하루가 끝난 지 7일 넘게 지난 날)만 저장해서 지우는 로직이 없어요.
-- timezone이 지금 사용자 시간대와 다르면 쓰지 않고 새로 계산해 덮어써요.
CREATE TABLE daily_activity
(
    user_id     BIGINT      NOT NULL,
    date        DATE        NOT NULL,
    timezone    VARCHAR(64) NOT NULL,
    seconds     INT         NOT NULL,
    slots       BIGINT      NOT NULL COMMENT '0시부터 30분 칸 48개를 비트로 (i번 비트 = i번 칸)',
    computed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, date),
    CONSTRAINT fk_daily_activity_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
