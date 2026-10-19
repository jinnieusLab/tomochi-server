-- 앱이 보낸 작업 구간을 clientSegmentId별로 그대로 저장해요.
-- 여러 기기의 구간이 겹쳐도 여기서는 합치지 않고, 읽을 때 합집합으로 계산해요 (docs/decisions/0001).
CREATE TABLE activity_segment
(
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    user_id           BIGINT      NOT NULL,
    client_segment_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    started_at        DATETIME(6) NOT NULL,
    ended_at          DATETIME(6) NOT NULL,
    created_at        DATETIME(6) NOT NULL,
    updated_at        DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_activity_segment_client_id UNIQUE (user_id, client_segment_id),
    INDEX idx_activity_segment_user_started (user_id, started_at),
    CONSTRAINT fk_activity_segment_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_activity_segment_range CHECK (ended_at > started_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
