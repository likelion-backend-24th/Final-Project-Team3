# 문서 원본·Git Snapshot 인덱스

이 파일은 팀 Repository의 `docs/README.md`로 사용합니다. 첫날에는 아래 15종 Notion 원본 URL과 담당자만 등록하고, 15개 Markdown 본문은 만들지 않습니다. 이 인덱스는 15종 기준 문서 수에 포함하지 않으며 Git에서 직접 관리합니다.

## 원본·Snapshot 링크 목차

| 문서 | Notion 원본 URL | 담당자 | 최근 Git Snapshot(보존본) |
|---|---|---|---|
| 요구사항 | [https://www.notion.so/3cd73873401a8075b571cfd74f65236d?source=copy_link] | 김지선, 차시환, 전기혁, 정동욱 | [`docs/요구사항.md`](./요구사항.md) |
| 공통 완료 기준 | [https://www.notion.so/Definition_of_Done-3cd73873401a800999dcea3ad9079d9d?source=copy_link] | 김지선, 차시환, 전기혁, 정동욱 | 없음 — Sprint Review 뒤 `docs/Definition_of_Done.md` 생성 |
| 화면 설계 | [https://www.notion.so/3cd73873401a80e7ad4ff56e62a83a8b?source=copy_link] | 김지선, 차시환, 전기혁, 정동욱 | 없음 — Sprint Review 뒤 `docs/화면설계.md` 생성 |
| 서비스 경계 | [https://www.notion.so/3cd73873401a805da932d60b57917b75?source=copy_link] | 김지선, 차시환, 전기혁, 정동욱 | 없음 — Sprint Review 뒤 `docs/서비스경계.md` 생성 |
| 아키텍처 | [https://www.notion.so/3cd73873401a80d9be4ee24c173805a4?source=copy_link] | 김지선, 차시환, 전기혁, 정동욱 | 없음 — Sprint Review 뒤 `docs/아키텍처.md` 생성 |
| ERD | [https://www.notion.so/ERD-3cd73873401a80f7bb96d00743b0d10c?source=copy_link] | 김지선, 차시환, 전기혁, 정동욱 | 없음 — Sprint Review 뒤 `docs/ERD.md` 생성 |
| API | [https://www.notion.so/API-3cd73873401a80bda31be290a53d6db4?source=copy_link] | 김지선, 차시환, 전기혁, 정동욱 | 없음 — Sprint Review 뒤 `docs/API.md` 생성 |
| 권한 Matrix | [https://www.notion.so/3cd73873401a80d9b181c4bb3ffef6b6?source=copy_link] | 김지선, 차시환, 전기혁, 정동욱 | 없음 — Sprint Review 뒤 `docs/권한매트릭스.md` 생성 |
| 시퀀스 | [https://www.notion.so/3cd73873401a8002a389e5ee51bebf20?source=copy_link] | 김지선, 차시환, 전기혁, 정동욱 | 없음 — Sprint Review 뒤 `docs/시퀀스.md` 생성 |
| 테스트 전략 | [https://www.notion.so/3cd73873401a80a4814eebfdaca94034?source=copy_link] | 김지선, 차시환, 전기혁, 정동욱 | 없음 — Sprint Review 뒤 `docs/테스트전략.md` 생성 |
| 테스트 체크리스트 | [https://www.notion.so/3cd73873401a80e6a4dbd348d8fc18e3?source=copy_link] | 김지선, 차시환, 전기혁, 정동욱 | 없음 — Sprint Review 뒤 `docs/테스트체크리스트.md` 생성 |
| 실행·배포 가이드 | [https://www.notion.so/3cd73873401a80b6becdcbd632d11299?source=copy_link] | 김지선, 차시환, 전기혁, 정동욱 | 없음 — Sprint Review 뒤 `docs/배포가이드.md` 생성 |
| 트러블슈팅 | [https://www.notion.so/3cd73873401a8080bac4f7655723d607?source=copy_link] | 김지선, 차시환, 전기혁, 정동욱 | 없음 — Sprint Review 뒤 `docs/트러블슈팅.md` 생성 |
| Sprint Review | [https://www.notion.so/Sprint-Review-3cd73873401a800c96f3dd0dd6f04edd?source=copy_link] | 김지선, 차시환, 전기혁, 정동욱 | 없음 — Sprint Review 뒤 `docs/스프린트리뷰.md` 생성 |
| Sprint Retrospective | [https://www.notion.so/retrospective-3cd73873401a804d9226f417b853bb90?source=copy_link] | 김지선, 차시환, 전기혁, 정동욱 | 없음 — Sprint Review 뒤 `docs/retrospective.md` 생성 |

## 첫날 확인

- [x] 15종 Notion 페이지가 있고 프로젝트 검토자가 열람할 수 있습니다.
- [x] 각 페이지 URL과 담당자가 위 표에 등록되어 있습니다.
- [x] 팀 Repository의 `README.md`가 이 인덱스와 GitHub Project·Issue를 연결합니다.
- [x] Git `docs/`에는 아직 15개 본문을 만들지 않았습니다.

## Sprint Review 뒤 동기화

1. Review 결과를 먼저 Notion 원본에 반영합니다.
2. 확정된 15종 현재 내용을 같은 `docs/*.md` 경로에 복사합니다.
3. 각 Snapshot 상단에 Notion 원본 URL·Snapshot 기준 시점·동기화 시각·직접 편집 금지를 기록합니다.
4. 위 표의 최근 Git Snapshot을 실제 상대 링크로 바꾸고 문서 PR을 만듭니다.
5. 수정이 필요하면 Git 파일을 직접 고치지 않고 `Notion 수정 → Git 재동기화` 순서를 지킵니다.

별도 Sprint 폴더를 만들지 않습니다. Sprint별 내용은 Git commit 이력으로 보존하고 Week 4에 최종 Notion 내용을 다시 동기화합니다. Secret·Token·Cookie·개인정보는 Notion과 Git 어디에도 기록하지 않습니다.
