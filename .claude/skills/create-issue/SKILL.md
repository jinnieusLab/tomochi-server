---
name: create-issue
description: Use whenever creating a GitHub issue in this repository (gh issue create, "이슈 만들어줘"). Picks the right issue template and keeps the body short.
---

# 이슈 만들기

`.github/ISSUE_TEMPLATE/`의 템플릿 구조를 그대로 따르되, 내용은 **짧게** 채운다.

## 템플릿 고르기

| 상황 | 템플릿 | 제목 | 라벨 |
|---|---|---|---|
| 새 기능, 세부 작업 | `feat.md` | `feat: 기능명 - 세부 작업` | feat |
| 버그 | `fix.md` | `fix: 오류명 - 요약` | bug |
| 동작 변경 없는 구조 개선 | `refactor.md` | `refactor: 대상 - 요약` | refactor |
| 설정, 의존성, 스크립트 | `chore.md` | `chore: 작업 내용` | chore |
| 배포, 인프라 | `deploy.md` | `deploy: 버전/환경 - 요약` | deploy |

## 분량 규칙

- 제목은 한 줄, 40자 안팎.
- 각 섹션은 1~3줄. 문단으로 길게 쓰지 않는다.
- 작업 내용 체크리스트는 3~5개. 그보다 많으면 이슈를 나눈다 (기능 하나 = 작은 작업 하나).
- 구현 방법, 코드, 긴 배경 설명은 넣지 않는다. 필요하면 참고 사항에 링크나 한 줄로.
- 템플릿 안내 문구(`- 이 기능을 구현하려는 이유와...` 같은 설명)는 지우고 실제 내용으로 바꾼다.
- 쓸 내용이 없는 섹션(예: 참고 사항)은 통째로 지운다.

## 만드는 방법

```bash
gh issue create --title "feat: 펫 캐릭터 - 16×16 픽셀 초코로 교체" --label feat --body "$(cat <<'BODY'
## 🎯 구현 목적
- 평면 초코를 확정 시안인 16×16 픽셀 캐릭터로 바꾼다.

## 📝 작업 내용
- [ ] 16×16 초코 픽셀 데이터 작성
- [ ] 정수 배율 확대, 보간 끄기
- [ ] 숨쉬기·찌그러짐 애니메이션과 연결
BODY
)"
```

- 라벨이 레포에 없어 실패하면 사용자에게 알리고, 라벨 없이 만들지 먼저 묻는다.
- 만든 뒤 이슈 URL을 사용자에게 알려준다.
