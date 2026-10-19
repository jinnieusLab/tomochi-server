---
name: create-pr
description: Use whenever creating a GitHub pull request in this repository (gh pr create, "PR 만들어줘"). Fills the PR template briefly and uses the project's PR title format.
---

# PR 만들기

`.github/pull_request_template.md` 구조를 그대로 따르되, 내용은 **짧게** 채운다.

## 제목

이모지 없이 `{Type}/#{이슈번호} {제목}` 형식. 커밋 메시지 형식(`✨ feat: ...`)과 혼동하지 않는다.

```
Feat/#3 16×16 픽셀 초코 캐릭터로 교체
Fix/#7 전체 화면에서 펫이 보이는 문제
Refactor/#9 AppState 저장 로직 분리
Chore/#2 이슈·PR 템플릿 추가
```

Type은 이슈 타입과 맞춘다: Feat / Fix / Refactor / Chore / Deploy / Docs / Test.

## 분량 규칙

- 개요는 1~2줄.
- 작업 내용은 3~5개 항목, 한 항목 한 줄. 파일별 나열이나 코드 설명은 하지 않는다.
- 스크린샷 / 테스트 결과는 UI·애니메이션 변경이나 테스트 로그가 있을 때만. 없으면 섹션을 지운다.
- 체크리스트는 실제로 확인한 항목만 체크한다. 해당 없는 항목(예: API가 없는 설정 작업의 API 동작 확인)은 지운다.
- 리뷰 포인트는 정말 봐줬으면 하는 것 1~2개만. 없으면 섹션을 지운다.
- 템플릿 안내 문구(`> 이번 PR에서 변경된...`)와 예시 문구는 지운다.

## 만드는 방법

1. 브랜치는 GitHub Flow: `main`에서 `{type}/#{이슈번호}` 브랜치를 만들어 작업한다 (예: `feat/#3`, `fix/#7`). 설명은 붙이지 않는다.
2. `git log main..HEAD`와 diff로 변경 내용을 확인하고 요약한다.
3. push 후 생성:

```bash
gh pr create --base main --title "Feat/#3 16×16 픽셀 초코 캐릭터로 교체" --body "$(cat <<'BODY'
## 📌 관련 이슈
- Close #3

## 🚀 개요
평면 초코를 16×16 픽셀 캐릭터로 교체했습니다.

## 📄 작업 내용
- 픽셀 데이터로 그리는 MochiBody 추가
- 정수 배율 확대, 보간 끔
- 기존 애니메이션과 연결

## ✅ 체크리스트
- [x] 브랜치 전략(GitHub Flow)을 준수했나요?
- [x] 앱 빌드·실행 확인
BODY
)"
```

- 관련 이슈 번호를 모르면 사용자에게 묻는다.
- 만든 뒤 PR URL을 사용자에게 알려준다.
