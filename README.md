# tomochi-server

토모찌 맥 앱의 백엔드예요. 로그인, 활동 기록, 친구, 꾹, 둘이 함께한 시간 집계를 맡아요.

## 기술 스택

Spring Boot 4.1 · Java 21 · Gradle · springdoc OpenAPI

## 실행

```bash
cp .env.example .env
./gradlew bootRun
```

- 기본 프로필은 `local`이에요. 다른 프로필은 `SPRING_PROFILES_ACTIVE=dev`처럼 환경 변수로 정해요.
- API 문서: http://localhost:8080/swagger-ui.html (`prod`에서는 꺼져 있어요)

## 설정

| 파일 | 내용 |
| --- | --- |
| `application.yml` | 공통 설정 |
| `application-{local,dev,prod}.yml` | 프로필별 설정 |
| `.env` | 로컬 비밀값. 커밋하지 않고 `.env.example`에 키 이름만 남겨요 |

비밀값(DB 비밀번호, JWT 키, OAuth 시크릿)은 환경 변수로만 넣어요. 레포가 나중에 public이 되기 때문에 커밋 히스토리에도 남기지 않아요.

## 패키지 구조

도메인별로 나누고, 도메인 안에서 `controller` · `service` · `repository` · `domain` · `dto`로 나눠요.

```text
com.tomochi
├── global      공통 설정, 예외 처리, 보안
├── auth        로그인, 토큰
├── user        내 정보
├── activity    작업 구간, 하루 기록
├── presence    작업 중 상태
├── friend      초대 코드, 친구 관계
├── squish      꾹
└── together    둘이 함께한 시간 집계
```
