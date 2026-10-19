# Dev 배포

VPS 1대에 Docker Compose로 앱·MySQL·Redis·Nginx·certbot을 띄워요.

```text
GitHub Actions ─ CI 통과 ─▶ 이미지 빌드 ─▶ GHCR ──pull──┐
              └──────────── SSH ─▶ VPS: deploy.sh ──────┘
VPS:  Nginx(80, 443) ─▶ app:8080 ─▶ mysql:3306, redis:6379   (밖으로는 Nginx만 열림)
```

| 파일 | 하는 일 |
| --- | --- |
| `Dockerfile` | `bootJar`로 만든 jar를 레이어로 풀어 실행 이미지로 |
| `deploy/docker-compose.yml` | VPS에서 띄우는 전체 구성 |
| `deploy/nginx/templates/default.conf.template` | HTTPS, http→https, WebSocket, `/actuator` 막기 |
| `deploy/init-letsencrypt.sh` | 처음 한 번 인증서 발급 |
| `deploy/deploy.sh` | 새 이미지 받아 다시 띄우기 (Actions가 실행) |
| `.github/workflows/deploy-dev.yml` | main CI 통과 → 이미지 → 배포 |

## 왜 이렇게

- **이미지는 Actions에서 만들어요.** 작은 VPS에서 Gradle 빌드를 돌리면 메모리가 모자라고 배포 중 서비스가 느려져요. VPS는 이미지를 받기만 해요.
- **태그는 커밋 해시(`sha-1a2b3c4`)로 남겨요.** `dev` 태그는 항상 최신이고, 문제가 생기면 `IMAGE_TAG=sha-이전커밋 ./deploy.sh`로 되돌려요.
- **레지스트리 로그인은 배포할 때만 해요.** 실행마다 새로 주는 `GITHUB_TOKEN`으로 로그인하고 바로 로그아웃해서, VPS에 오래 쓰는 토큰을 두지 않아요.
- **비밀값은 VPS의 `deploy/.env`에만 있어요.** 레포와 Actions 로그에는 남지 않아요.

## 처음 한 번 준비하기

### 1. VPS

- Ubuntu 24.04, 메모리 2GB 이상 (MySQL + JVM)
- 방화벽은 22, 80, 443만 열어요.
- Docker Engine과 Compose 플러그인 설치: https://docs.docker.com/engine/install/ubuntu/
- 배포용 사용자를 만들고 docker 그룹에 넣어요.

```bash
sudo adduser --disabled-password deploy
sudo usermod -aG docker deploy
```

### 2. SSH 키 (내 맥에서)

```bash
ssh-keygen -t ed25519 -f ~/.ssh/tomochi_deploy -C "github-actions-deploy"
```

공개 키(`~/.ssh/tomochi_deploy.pub`)를 VPS의 `/home/deploy/.ssh/authorized_keys`에 넣어요.

### 3. 도메인

Dev 도메인(예: `dev-api.도메인`)의 A 레코드를 VPS IP로 잡아요.

### 4. VPS에 배포 폴더와 `.env`

```bash
scp -r deploy/docker-compose.yml deploy/deploy.sh deploy/init-letsencrypt.sh deploy/nginx deploy/.env.example deploy@VPS_IP:~/tomochi/
ssh deploy@VPS_IP
cd ~/tomochi && cp .env.example .env && chmod 600 .env
```

`.env`의 `DOMAIN`, `CERTBOT_EMAIL`, DB 비밀번호를 채워요. 비밀번호는 `openssl rand -base64 24`로 만들어요.

### 5. 인증서

```bash
STAGING=1 ./init-letsencrypt.sh   # 시험용으로 먼저 확인
./init-letsencrypt.sh             # 진짜 인증서
```

### 6. GitHub 설정

Settings → Environments에서 `dev` 환경을 만들고 Secrets를 넣어요.

| 이름 | 값 |
| --- | --- |
| `DEV_SSH_HOST` | VPS IP 또는 도메인 |
| `DEV_SSH_USER` | `deploy` |
| `DEV_SSH_KEY` | `~/.ssh/tomochi_deploy` (개인 키) 내용 |
| `DEV_SSH_KNOWN_HOSTS` | 내 맥에서 `ssh-keyscan VPS_IP` 결과 |

마지막으로 Settings → Variables에 `DEPLOY_DEV_ENABLED` = `true`를 넣으면 다음 main 머지부터 배포가 돌아요. 이 변수가 없으면 배포 워크플로는 건너뛰어요.

## 확인

```bash
curl -I https://dev-api.도메인/v3/api-docs   # 200
curl -I http://dev-api.도메인/               # 301 → https
curl -I https://dev-api.도메인/actuator      # 404 (밖에서 막힘)
```
