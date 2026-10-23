#!/usr/bin/env bash
# 부하 테스트 DB를 비우고 테스트 사용자·친구 관계를 넣어요. Redis(상태·요청 제한)도 비워요.
#   loadtest/seed.sh            # 사용자 1,000명, 1명당 친구 10명
#   loadtest/seed.sh 2000 5     # 사용자 2,000명, 1명당 친구 10명(한쪽 5명)
set -euo pipefail

USERS="${1:-1000}"
HALF="${2:-5}"
DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE=(docker compose -f "$DIR/docker-compose.yml")

if (( HALF < 1 || HALF > 10 || USERS <= 2 * HALF )); then
	echo "친구는 1명당 최대 20명이에요 (한쪽 1~10), 사용자 수는 2×한쪽보다 커야 해요" >&2
	exit 1
fi

{
	echo "SET @users = $USERS; SET @half = $HALF;"
	cat "$DIR/seed.sql"
} | "${COMPOSE[@]}" exec -T mysql sh -c 'mysql -utomochi -p"$MYSQL_PASSWORD" tomochi' 2>/dev/null

"${COMPOSE[@]}" exec -T redis redis-cli FLUSHALL >/dev/null
echo "Redis 비움"
