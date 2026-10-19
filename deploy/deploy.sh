#!/usr/bin/env bash
# 새 이미지를 받아 다시 띄워요. GitHub Actions가 SSH로 실행해요.
#   IMAGE_TAG=sha-1a2b3c4 ./deploy.sh
set -euo pipefail
cd "$(dirname "$0")"

export IMAGE_TAG="${IMAGE_TAG:-dev}"
echo "▶ ghcr.io/jinnieuslab/tomochi-server:$IMAGE_TAG 배포"

docker compose pull app
docker compose up -d --remove-orphans
docker image prune -f > /dev/null

docker compose ps
