# 모니터링·로그

```text
밖 ─▶ Nginx(80, 443) ─▶ app:8080 (API)
                         app:8081 (Actuator) ◀── Prometheus(15초마다) ◀── Grafana(127.0.0.1:3000)
```

| 무엇 | 어디서 | 밖에서 |
| --- | --- | --- |
| API | `app:8080` | Nginx로만 |
| Actuator `health`, `info`, `prometheus` | `app:8081` (관리 포트) | 닿지 않음 |
| Prometheus | `prometheus:9090` | 닿지 않음 |
| Grafana | VPS의 `127.0.0.1:3000` | SSH 터널로만 |

## 왜 이렇게

- **Actuator는 다른 포트에서만 열어요.** Nginx는 8080만 넘기고 8081은 Docker 네트워크 밖으로 내보내지 않아서, Nginx 설정을 실수해도 지표가 밖으로 새지 않아요. 그래서 관리 포트는 인증 없이 Prometheus가 바로 읽어요.
- **Grafana는 공개하지 않아요.** 혼자 보는 대시보드라 로그인 화면을 인터넷에 둘 이유가 없어요. VPS 안에서만 열고 SSH 터널로 봐요.
- **지금 볼 수 있는 지표**: JVM 메모리·GC·스레드, HTTP 요청 수·지연(`http_server_requests_seconds`), DB 커넥션 풀(`hikaricp_*`). 모든 지표에 `application="tomochi-server"` 태그가 붙어요.
- **나중에 붙일 지표** (기능과 함께): WebSocket 연결 수(RT-1), 꾹 전달 지연(SQSH-2), 구간 업로드 거절·잘림 수(ACT-1), 집계 배치 소요 시간·처리 쌍 수(TGTR-1).

## Grafana 보기

```bash
ssh -L 3000:127.0.0.1:3000 deploy@VPS_IP
```

브라우저에서 http://localhost:3000 → `admin` / `.env`의 `GRAFANA_ADMIN_PASSWORD`. Prometheus 데이터 소스는 자동으로 붙어 있어요. 처음엔 Dashboards → New → Import에서 JVM(Micrometer) 대시보드 ID `4701`을 가져오면 편해요.

로컬에서는 `./gradlew bootRun` 후 http://localhost:8081/actuator/health, http://localhost:8081/actuator/prometheus 로 바로 볼 수 있어요.

## 로그 규칙

레포와 로그는 나중에 공개될 수 있다고 보고 남겨요.

- **남기지 않는 것**: Access·Refresh Token, 1회용 로그인 코드, OAuth `code`·`state`, 초대 코드 전체, 이메일, 비밀번호·키 같은 설정 값, 요청·응답 본문.
- **사용자는 id로만** 남겨요 (`userId=12`). 닉네임도 남기지 않아요.
- **Nginx 접속 기록에는 쿼리 문자열을 빼요** (`$request` 대신 `$uri`). 로그인 콜백 `?code=…&state=…`이 로그 파일에 쌓이지 않게 해요.
- **소셜 로그인 실패는 오류 코드만** 남겨요. 제공자가 준 설명에는 요청 값이 섞일 수 있어요.
- 새로 `log.*`를 쓸 때는 위 목록에 걸리는 값이 인자로 들어가지 않는지 봐 주세요 (CodeRabbit 리뷰 기준에도 들어 있어요).
