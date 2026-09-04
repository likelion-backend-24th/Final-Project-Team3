# 공통 완료 기준(Definition of Done, DoD)

> **작성·동기화 메타정보**
> 
> 
> Notion 원본 URL: `https://app.notion.com/p/3-3c973873401a8045ac7ee8adcaf2b71a`
> 
> Snapshot 기준 시점: `Sprint 1 Review 종료 시점 (2026-09-04)`
> 
> 동기화 시각: `2026-09-04 18:30 KST`
> 
> 직접 편집 금지: Git Snapshot은 직접 편집하지 않고 Notion 원본을 수정한 뒤 다시 동기화합니다.
> 

공통 완료 기준은 Product Backlog Item을 완료된 제품 결과로 인정하기 위해 반드시 만족해야 하는 팀 공통 품질 기준입니다. 팀은 Sprint 시작 전에 기준을 합의하고 항목별 증거를 남깁니다.

## 공통 완료 조건

PBI는 적용 대상인 다음 조건을 모두 만족해야 Done입니다.

- [ ]  Acceptance Criteria를 모두 만족한다.
- [ ]  핵심 단위·통합 Test와 관련 회귀 Test가 통과한다.
- [ ]  정상 흐름과 주요 실패 흐름을 확인한다.
- [ ]  인증·인가·소유권과 민감정보 노출 여부를 확인한다.
- [ ]  다른 구성원이 Code Review를 완료했다.
- [ ]  Schema 변경 시 Migration과 롤백·호환 영향을 검토했다.
- [ ]  API 변경 시 OpenAPI와 소비자 계약을 갱신했다.
- [ ]  요구사항·설계·운영 문서와 링크를 현재 구현에 맞췄다.
- [ ]  통합 환경에서 실제로 실행하고 Sprint Review에서 시연할 수 있다.
- [ ]  치명적 결함이 남아 있지 않다.
- [ ]  Issue에 실행 명령·결과·Log 또는 화면 등 재현 가능한 Evidence를 연결했다.

적용되지 않는 조건은 체크를 생략하지 말고 `N/A — 사유`를 남깁니다. 기준을 만족하지 못한 작업은 진행률과 무관하게 완료된 제품 결과가 아닙니다.

## PBI별 Evidence

| PBI | AC | Test·회귀 | 권한·민감정보 | Review | 계약·문서 | 통합·Demo | Evidence | 판정 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Story 1 (`#1`) 방문자 검색 | 목록/상세 조회, 승인된 것만 노출 — 충족 | 백엔드 Task 1-3 Acceptance Test(#5, CLOSED) 존재. 프론트 자동 테스트는 없음(수동 스모크만) | APPROVED만 조회됨 — 오늘 직접 확인(GET /api/conferences PENDING 안 섞임) | PR #45, #41(백엔드), #64(프론트) — 팀원 승인 여부는 GitHub PR 탭에서 확인 필요 | Task 1-2 API [`#4`] | 실제 통합 환경(로컬 4서비스)에서 실행+시연 가능 확인함 | Conference Service CI 최근 실행 전부 success | NOT_DONE |
| Story 2 (`#3`) 회원가입  | 가입/로그인 성공·실패 흐름 확인 — 충족 | Task 2-2 Acceptance Test(#25, CLOSED) 존재 | BCrypt(strength 12) 확인, 이메일 unique+애플리케이션단 이중 체크 확인 | PR #42(백엔드), #64(프론트) | Task 2-1 API [`#9`]  | 실제 계정 가입→로그인→새로고침 세션 유지까지 확인 | Member-Service CI success | NOT_DONE  |
| Story 3 (`#26`) 주최자 자체 회원가입 | [[링크]](https://github.com/likelion-backend-24th/Final-Project-Team3/issues/35)  | 구현 후 링크 추가 예정 | 사업자등록번호 검증·중복 확인 | PR | Task 3-1 signupOrganizer API #31 | Demo 링크 | CI 로그 | NOT_DONE |
| Story 4 (`#28`) 주최자 로그인  | role=ORGANIZER 분기, 로그아웃 — 충족 | Task 8-3 Acceptance Test | OwnerScopeGuard로 권한 스코프 제한(PR #54) — 코드상 존재 확인, 실사용 시나리오 재현은 아직 안 함 | PR #53, #54, #64 | Task 4-1 로그인 흐름 [`#30`]  | test@exam.com 계정으로 로그인→대시보드 진입 직접 확인 | Member-Service CI success | NOT_DONE  |
| Story 5 (`#35`) 컨퍼런스 등록 신청  | organizerName/기간/장소/소개/태그까지 실제 저장 | Task 9-2 Acceptance Test PR #67 존재·CI success | 신청자 본인(JWT organizerId) 기준 저장 확인 | PR #57, #65, #67, 프론트 a16b77d | Task 5-1 API [`#36`]  | 폼 입력→제출→DB 저장까지 실제 e2e 확인 | Conference Service CI success | NOT_DONE  |
| Story 6 (`#27`) 승인·반려  | [[링크]](https://github.com/likelion-backend-24th/Final-Project-Team3/issues/27)  | [구현 후 링크 추가 예정] | 전체관리자만 승인 가능 
확인  | [PR]  | Task 6-1,6-2 API·연동 [`#38`, `#39`]  |  [Demo 링크]  | [CI 로그]  | NOT_DONE  |
| Story 9 (`#20`) 세션 신청·홀드  | HOLD/QUEUED 분기 — 오늘 정원 1명 세션으로 QUEUED까지 재현 확인 | Task 3-2 동기 계약(#23, CLOSED), k6 부하 테스트 PR #66 존재·CI success | 오늘은 memberId를 body로 직접 받는 구조라(JWT 미검증) 본인 확인 안 됨 | PR #43, #46, #48, #66 | Task 9-2 동기 
계약 [`#23`]  | 오늘 실제 hold API로 HOLD/QUEUED 둘 다 재현 | Reservation Service CI success | NOT_DONE  |
| Story 10 (`#6`) 대기열  | 순번 조회 —  QUEUED 후 순번 화면 진입 확인 | [구현 후 링크 추가 예정] |  본인 신청만 순번 조회 확인 안 됨(위와 동일 갭) | PR #56 | Task 10-1 API [`#7`]  | 오늘 실제 대기열 등록→순번 화면 확인 | Reservation Service CI success | NOT_DONE  |

## 별도 품질평가와의 관계

Week 4의 `PASS/REWORK_REQUIRED`는 품질평가 담당자가 산출물 품질과 프로젝트 기준을 확인하는 별도 품질 Gate입니다. 별도 품질평가가 `PASS`여도 완료 기준을 만족하지 않은 PBI가 Done으로 바뀌지는 않으며, Sprint Review 또한 릴리스를 허가하는 Gate가 아닙니다.