-- 부하 테스트용 사용자와 친구 관계. 부하 테스트 DB(loadtest/docker-compose.yml)에서만 써요.
-- seed.sh가 @users, @half 값을 앞에 붙여서 실행해요.
--   @users: 사용자 수 (id 1..@users)
--   @half : 한쪽 이웃 수. 사용자 i는 i±1..i±@half와 친구라서 친구가 2×@half명이에요 (최대 20명 → @half ≤ 10)
-- 원처럼 이어 붙여서 모든 사용자의 친구 수가 같아요. 그래야 사용자마다 일의 양이 같아 결과를 읽기 쉬워요.

SET SESSION cte_max_recursion_depth = 100000;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE friend_setting;
TRUNCATE TABLE friendship;
TRUNCATE TABLE squish;
TRUNCATE TABLE activity_segment;
TRUNCATE TABLE daily_activity;
TRUNCATE TABLE together_daily;
TRUNCATE TABLE refresh_token;
TRUNCATE TABLE oauth_account;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO users (id, nickname, pet_kind, status_icon, timezone, invite_code, created_at, updated_at)
WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < @users)
SELECT n,
       CONCAT('u', n),
       ELT(1 + MOD(n, 6), 'CHOCO', 'BUTTER', 'HOBBANG', 'SHERBET', 'PEANUT', 'HONEYCOMB'),
       ELT(1 + MOD(n, 6), 'STUDY', 'CODE', 'WRITE', 'DESIGN', 'MUSIC', 'FOCUS'),
       'Asia/Seoul',
       LPAD(CONV(n, 10, 36), 6, '0'),
       NOW(6),
       NOW(6)
FROM seq;

INSERT INTO friendship (user_low, user_high, created_at)
WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < @users),
               k (d) AS (SELECT 1 UNION ALL SELECT d + 1 FROM k WHERE d < @half)
SELECT LEAST(n, MOD(n - 1 + d, @users) + 1),
       GREATEST(n, MOD(n - 1 + d, @users) + 1),
       NOW(6)
FROM seq
         CROSS JOIN k;

SELECT (SELECT COUNT(*) FROM users) AS users, (SELECT COUNT(*) FROM friendship) AS friendships;
