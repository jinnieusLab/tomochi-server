-- 꾹. delivered_at이 비어 있으면 아직 앱에 전달되지 않은 꾹이에요 (앱이 꺼져 있던 동안의 꾹, SQSH-3).
CREATE TABLE squish
(
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    from_user_id BIGINT      NOT NULL,
    to_user_id   BIGINT      NOT NULL,
    sent_at      DATETIME(6) NOT NULL,
    delivered_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_squish_to_delivered (to_user_id, delivered_at),
    INDEX idx_squish_from (from_user_id),
    CONSTRAINT fk_squish_from FOREIGN KEY (from_user_id) REFERENCES users (id),
    CONSTRAINT fk_squish_to FOREIGN KEY (to_user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
