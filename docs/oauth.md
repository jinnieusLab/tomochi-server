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

## 토큰 재발급 (Refresh Token 회전)

`POST /api/v1/auth/refresh { refreshToken }` → 새 `{ accessToken, refreshToken, expiresIn }`. 쓴 Refresh Token은 바로 폐기돼요.

```text
로그인 ─▶ A ──회전──▶ B ──회전──▶ C        (A, B, C는 같은 family = 같은 기기)
공격자가 훔친 A를 나중에 쓰면 ─▶ family 전체 폐기 → 앱도 다시 로그인 (403 REFRESH_TOKEN_REUSED)
```

| 상황 | 응답 | family |
| --- | --- | --- |
| 살아 있는 토큰 | 200 새 토큰 한 쌍 | 이어짐 |
| 없거나 만료된 토큰 | 401 `REFRESH_TOKEN_INVALID` | 그대로 |
| 10초 안에 회전된 토큰 (같은 토큰으로 보낸 동시 요청) | 409 `REFRESH_TOKEN_ROTATED` | 그대로 |
| 그 밖의 폐기된 토큰 (재사용) | 403 `REFRESH_TOKEN_REUSED` | 모두 폐기 |

### 왜 이렇게

- **회전 + 재사용 감지.** 맥 앱은 몇 주씩 켜 두니 Refresh Token 수명이 길어야 해요(30일). 대신 쓸 때마다 바꾸고, 이미 폐기된 토큰이 다시 오면 "둘 중 하나는 훔친 사람"이라 그 기기의 토큰을 모두 끊어요. 앱과 공격자 중 누가 먼저 썼든 다음 회전에서 들통나요.
- **동시 요청은 락 대신 조건부 UPDATE로 정해요.** `UPDATE refresh_token SET revoked_at = now WHERE id = ? AND revoked_at IS NULL`. InnoDB가 이 행을 잠그고 차례로 실행해서 영향 받은 행이 1인 요청은 하나뿐이에요. 그 요청만 새 토큰을 만들어요 (`RefreshTokenConcurrencyTest`: 10개 동시 → 1개 성공, 9개 409).
- **진 요청은 잠금 읽기로 다시 봐요.** MySQL 기본 격리 수준(REPEATABLE READ)에서 평범한 SELECT는 트랜잭션 처음의 스냅샷을 읽어서, 방금 이긴 요청이 만든 새 토큰이 안 보여요. `SELECT ... FOR UPDATE`는 지금 커밋된 값을 읽어서 "방금 회전됐는지"를 정확히 판단해요.
- **10초 유예.** 같은 토큰으로 거의 동시에 온 요청(앱이 재발급을 두 번 보냄)을 탈취로 보면 정상 사용자가 로그아웃돼요. 회전된 지 10초 안이고 family에 살아 있는 다음 토큰이 있을 때만 409로 봐 주고, 그 밖에는 탈취로 봐요.
- **실패 응답이어도 폐기는 남겨요.** 재사용을 감지하면 family를 폐기한 뒤 403을 던지는데, 예외로 트랜잭션이 롤백되면 폐기도 사라져요. 그래서 `@Transactional(noRollbackFor = BusinessException.class)`예요.
