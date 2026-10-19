#!/usr/bin/env bash
# 처음 한 번만 실행해요. Let's Encrypt 인증서를 받아 Nginx에 붙여요.
# Nginx는 인증서 파일이 없으면 뜨지 않아서, 임시 인증서로 먼저 띄운 뒤 진짜로 바꿔요.
#   ./init-letsencrypt.sh          진짜 인증서
#   STAGING=1 ./init-letsencrypt.sh  시험용 인증서 (발급 횟수 제한에 안 걸려요)
set -euo pipefail
cd "$(dirname "$0")"

set -a; source .env; set +a
: "${DOMAIN:?.env에 DOMAIN을 넣어 주세요}"
: "${CERTBOT_EMAIL:?.env에 CERTBOT_EMAIL을 넣어 주세요}"

live="certbot/conf/live/$DOMAIN"
mkdir -p "$live" certbot/www

echo "▶ 임시 인증서 만들기"
docker compose run --rm --entrypoint "openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
  -keyout /etc/letsencrypt/live/$DOMAIN/privkey.pem \
  -out /etc/letsencrypt/live/$DOMAIN/fullchain.pem \
  -subj /CN=localhost" certbot

echo "▶ Nginx 띄우기"
docker compose up -d nginx

echo "▶ 임시 인증서 지우고 진짜 인증서 받기"
docker compose run --rm --entrypoint "rm -rf /etc/letsencrypt/live/$DOMAIN \
  /etc/letsencrypt/archive/$DOMAIN /etc/letsencrypt/renewal/$DOMAIN.conf" certbot

staging_arg=""
if [ "${STAGING:-0}" = "1" ]; then staging_arg="--staging"; fi

docker compose run --rm --entrypoint "certbot certonly --webroot -w /var/www/certbot \
  $staging_arg -d $DOMAIN --email $CERTBOT_EMAIL --agree-tos --no-eff-email --non-interactive" certbot

echo "▶ Nginx에 새 인증서 반영"
docker compose exec nginx nginx -s reload
echo "✔ https://$DOMAIN"
