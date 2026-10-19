# 소셜 로그인

## 흐름

```text
맥 앱                          서버                                  Google·GitHub
 │ verifier(무작위) 만들기
 │ challenge = BASE64URL(SHA256(verifier))
 │── 브라우저: GET /api/v1/auth/oauth2/{provider}?challenge=…&petKind=… ─▶│
 │                              │ 로그인 요청(state, challenge)을 Redis에 10분 저장
 │                              │──────── 로그인 페이지로 보내기 ──────────▶│
 │                              │◀─── /api/v1/auth/oauth2/callback/{provider}?code&state
 │                              │ state로 Redis에서 꺼내고 지움 (GETDEL)
 │                              │ 사용자 찾기/만들기, 1회용 코드 Redis 60초
 │◀── tomochi://auth/callback?code=… ───────│
 │── POST /api/v1/auth/token { code, codeVerifier, deviceName } ─▶│
 │                              │ GETDEL로 코드 꺼내기 → SHA256(verifier) == challenge 확인
 │◀── { accessToken(JWT 30분), refreshToken(30일), expiresIn } ──│
```

## 왜 이렇게

- **URL에 토큰을 싣지 않아요.** `tomochi://` 같은 커스텀 스킴은 다른 앱도 등록할 수 있어서 가로챌 수 있어요. URL에는 60초짜리 1회용 코드만 싣고, 토큰으로 바꿀 때 로그인을 시작한 앱만 아는 `codeVerifier`를 요구해요 (PKCE와 같은 원리). 코드가 새도 verifier 없이는 쓸 수 없어요.
- **코드는 꺼내면서 지워요.** Redis `GETDEL` 한 번이라 같은 코드로 동시에 두 번 와도 하나만 성공해요. verifier가 틀려도 코드는 이미 지워져서, 같은 코드로 verifier를 여러 번 맞혀 볼 수 없어요.
- **로그인 중 상태는 세션 대신 Redis에 둬요.** 키가 OAuth2 `state`라 돌아온 요청의 state로만 꺼낼 수 있고(CSRF 방지), 서버가 무상태라 여러 대로 늘어나도 그대로 동작해요.
- **계정은 `(제공자, 제공자 안의 id)`로 찾아요.** Google `sub`, GitHub `id`는 바뀌지 않아요. 이메일은 바뀔 수 있고 필요도 없어서 받지도 저장하지도 않아요. 같은 사람이라도 제공자가 다르면 다른 계정이에요.
- **첫 로그인이 동시에 와도 계정은 하나예요.** 둘 다 "없음"을 보고 만들려고 하면 `oauth_account (provider, provider_user_id)` 유니크 제약이 하나를 실패시키고, 실패한 쪽은 새 트랜잭션에서 먼저 만들어진 계정을 다시 찾아요. 락 없이 DB 제약으로 해결해요 (`OAuthLoginConcurrencyTest`).
- **Access Token은 Spring Resource Server가 검증해요.** HS256 서명, 만료, 발급자(`iss=tomochi`)를 확인하고, 만료면 `TOKEN_EXPIRED`, 나머지는 `TOKEN_INVALID`로 응답해요. 만료는 서명이 맞는 토큰에서만 검사하니 위조 토큰이 `TOKEN_EXPIRED`로 보이지 않아요.

## OAuth 앱 등록

환경(로컬, Dev)마다 콜백 주소가 달라서 앱을 따로 만들어요. 발급받은 값은 `.env`에만 넣어요.

| 환경 | 콜백 주소 |
| --- | --- |
| 로컬 | `http://localhost:8080/api/v1/auth/oauth2/callback/google` · `…/callback/github` |
| Dev | `https://{Dev 도메인}/api/v1/auth/oauth2/callback/google` · `…/callback/github` |

### Google

1. https://console.cloud.google.com → 프로젝트 만들기
2. API 및 서비스 → OAuth 동의 화면: 외부, 앱 이름 "토모찌", 범위는 `openid`만
3. 사용자 인증 정보 → OAuth 클라이언트 ID → 웹 애플리케이션, 승인된 리디렉션 URI에 위 콜백 주소
4. `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`

### GitHub

1. https://github.com/settings/developers → New OAuth App
2. Homepage URL은 아무 주소, Authorization callback URL에 위 콜백 주소
3. Generate a new client secret
4. `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`

## 로컬에서 확인

```bash
# verifier와 challenge 만들기
VERIFIER=$(openssl rand -base64 48 | tr '+/' '-_' | tr -d '=\n' | cut -c1-64)
CHALLENGE=$(printf %s "$VERIFIER" | openssl dgst -sha256 -binary | openssl base64 | tr '+/' '-_' | tr -d '=\n')
open "http://localhost:8080/api/v1/auth/oauth2/github?challenge=$CHALLENGE&petKind=HOBBANG"
# 로그인 후 브라우저가 tomochi://auth/callback?code=… 를 열려고 해요. 주소창의 code를 복사해서:
curl -X POST localhost:8080/api/v1/auth/token -H 'Content-Type: application/json' \
  -d "{\"code\":\"$CODE\",\"codeVerifier\":\"$VERIFIER\",\"deviceName\":\"내 맥북\"}"
```
