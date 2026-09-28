# 공통 완료 기준(Definition of Done, DoD)

> **작성·동기화 메타정보**
> 
> 
> Notion 원본 URL: `https://app.notion.com/p/3-3c973873401a8045ac7ee8adcaf2b71a`
> 
> Snapshot 기준 시점: `프로젝트 완료 시점 (2026-09-28)`
> 
> 동기화 시각: `2026-09-28 20:19 KST`
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
| Story 1 (#1) 방문자 검색 | 목록/상세 조회, 승인된 것만 노출 — 충족 | 백엔드 Task 1-3 Acceptance Test(#5, CLOSED) 존재. 프론트 자동 테스트는 없음(수동 스모크만) | APPROVED만 조회됨 — 오늘 직접 확인(GET /api/conferences PENDING 안 섞임) | PR #45, #41(백엔드), #64(프론트)  | Task 1-2 API #4 | 9월 10일
실제 통합 환경(로컬 4서비스)에서 실행+시연 가능 확인
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/34436747449 | Conference Service CI 최근 실행 전부 success
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/33605389189
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/33700540139 | DONE |
| Story 2 (#3) 회원가입 | 가입/로그인 성공·실패 흐름 확인 — 충족 | Task 2-2 Acceptance Test(#25, CLOSED) 존재 | BCrypt(strength 12) 확인, 이메일 unique+애플리케이션단 이중 체크 확인 | PR #42(백엔드), #64(프론트) | Task 2-1 API #9 | 9월 10일
실제 계정 가입→로그인→새로고침 세션 유지까지 확인
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/34436747449 | Member-Service CI success
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/33603014932 | DONE |
| Story 3 (#26) 주최자 자체 회원가입 | 사업자등록번호 인증 통과 시 ORGANIZER 계정 생성, 실패 시 400, 중복 시 409 — 테스트로 확인 | OrganizerSignupTest, OrganizerBusinessVerificationTest, OrganizerLoginTest(member-service). 2026-09-28 ./gradlew test --tests "*Organizer*" BUILD SUCCESSFUL | 사업자등록번호 형식 검증·이메일/사업자번호 중복 409 테스트로 확인 | PR #76 | Task 3-1 signupOrganizer API #31 | 9월 10일
실제 주최자 계정 가입→로그인→새로고침 세션 유지까지 확인
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/34436747449 | Member-Service CI success
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/34089396714 | DONE |
| Story 4 (#28) 주최자 로그인 | role=ORGANIZER 분기, 로그아웃 — 충족 | Task 4-3 Acceptance Test | OwnerScopeGuard로 권한 스코프 제한(PR #54). SessionOwnerScopeTest(단위 6개)로 타 주최자 컨퍼런스의 세션 등록·수정·목록 조회 거부 검증, 2026-09-28 ./gradlew test BUILD SUCCESSFUL | PR #53, #54, #64 | Task 4-1 로그인 흐름 #30 | 9월 10일
test@exam.com 계정으로 로그인→대시보드 진입 직접 확인
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/34436747449 | Member-Service CI success
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/33725469808 | DONE |
| Story 5 (#35) 컨퍼런스 등록 신청 | organizerName/기간/장소/소개/태그까지 실제 저장— 충족 | Task 9-2 Acceptance Test PR #67 존재·CI success | 신청자 본인(JWT organizerId) 기준 저장 확인 | PR #57, #65, #67, 프론트 a16b77d  | Task 5-1 API #36 | 9월 17일
폼 입력→제출→DB 저장까지 실제 e2e 확인
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/35182725644 | Conference Service CI success
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/34439140531
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/33823857334 | DONE |
| Story 6 (#27) 승인·반려 | 승인 시 참가자에게 공개, 반려 시 비공개, ADMIN 아니면 403 — 충족 | ConferenceApprovalAcceptanceTest 2026-09-28 ./gradlew test --tests "*ConferenceApproval*" BUILD SUCCESSFUL | 전체관리자(ADMIN)만 승인·반려 가능 확인 | PR #78, #79  | Task 6-1, 6-2, 6-3, 6-4, 6-5 API·연동 #38, #39 | 9월 17일
실제 주최자 신청→관리자 승인→참가자 목록 노출 확인
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/35182725644 | Conference-Service CI success
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/34090872106 | DONE |
| Story 6 Task 6-4/6-5 (#101/#133) 컨퍼런스 상세보기 | 승인 대기 컨퍼런스 상세(세션·태그·설명 포함) 조회, ADMIN 아니면 403, 없으면 404 — 3개 케이스 전부 검증 | AdminConferenceDetailTest(단위), ConferenceApprovalAcceptanceTest에 3개 케이스 추가 | ADMIN 역할만 조회 가능(@PreAuthorize) | PR #138(MERGED, Closes #133) | AdminConferenceController — API.md 계약과 일치 | 9월 17일
Docker 전체 스택(로컬 4서비스)에서 실제 브라우저로 Approvals.jsx 모달 렌더링까지 시연 확인
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/35182725644 | Conference-Service 테스트 스위트 BUILD SUCCESSFUL
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/34792620516 | DONE |
| Story 8 (#68) 세션 승인·반려 | 승인시 노출/반려시 비노출+사유저장/승인전 제외/권한없음 403 — SessionApprovalAcceptanceTest(#71)로 4개 전부 검증 | PendingSessionsQueryTest(#69), SessionApprovalTest(#70) 단위 테스트 + SessionApprovalAcceptanceTest(#71) 통합. 회귀: getConference가 세션 status 필터 없이 전체 노출하던 기존 버그 발견·수정, ConferenceVisibilityTest 갱신 | ADMIN 역할만 승인/반려 가능(@PreAuthorize) — 403/409 케이스까지 직접 확인 | PR #71 PR , #93(#69, MERGED), #94(#70, MERGED) | Task 8-1,8-2 API #69, #70 — API.md 계약과 일치 확인 | 9월 17일
Docker 전체 스택(MySQL)에서 세션 등록→승인 전 비로그인 비노출→관리자 승인·반려→방문자 화면에 승인된 세션만 노출 확인
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/35182725644 | Conference-Service 테스트 스위트 전체 build
success
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/34189913514 | DONE |
| Story 9 (#20) 세션 신청·홀드 | HOLD/QUEUED 분기 — 오늘 정원 1명 세션으로 QUEUED까지 재현 확인 | Task 3-2 동기 계약(#23, CLOSED), k6 부하 테스트 PR #66 존재·CI success | JWT(@AuthenticationPrincipal)에서 memberId를 꺼내 신청, 요청 body에 memberId 없음 — 코드로 확인 | PR #43, #46, #48, #66  | Task 9-2 동기 계약 #23 | 9월 17일
실제 hold API로HOLD/QUEUED 둘 다 재현
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/35182725644 | Reservation Service CI success
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/33703674039 | DONE |
| Story 10 (#6) 대기열 | 순번 조회 — QUEUED 후 순번·예상 대기시간 화면 진입 확인 | HoldConcurrencyTest(동시 100건 시 HOLD 정원·나머지 QUEUED), PaymentQrAcceptanceTest(대기열 순번 미도달 결제 거부·HOLD 만료 시 대기열 승계) 2026-09-28 ./gradlew test BUILD SUCCESSFUL | 순번 조회 시 예약 소유자와 로그인 memberId 비교, 다르면 거부 — 코드로 확인 | PR #56, #229, #256  | Task 10-1 API #7 | 9월 27일
오늘 실제 대기열 등록→순번 화면 확인
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/36267827667 | Reservation Service CI success
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/35810578879 | DONE |
| Story 11 (#73) 결제·QR 발급 | 결제 완료 시 좌석 확정 + QR 발급 — 충족, 동시요청 방어·HOLD 만료·대기열 승계까지 재현 검증 완료 | PaymentQrAcceptanceTest, headcountCapacityValidationTest, paymentCapacityRevalidationTest, holdExpirationPromotesQueueTest 전부 통과(CI success) | 결제 시 예약 소유자와 로그인 memberId 비교, 다르면 거부 — 코드로 확인 | PR #83, #99, #217  | Task 11-1~11-7(#73, #74, #75, #98, #102, #114, #115) API 계약 | 9월 21일
정원 1명 세션으로 새치기 버그 재현·수정 검증까지 완료(Docker+PowerShell 시나리오)
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/35547036530 | Reservation Service CI success
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/34205945538
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/35308233322 | DONE |
| Story 18 (#150) 통합 정산 대시보드 | GET /api/admin/settlements — CONFIRMED 건의 payment.amount 합산, CANCELLED 제외, ADMIN 아니면 403 — 전부 검증 | SettlementServiceTest(3개, 날짜필터 변환), SettlementAcceptanceTest(2개, CONFIRMED 30000+CANCELLED 99999 혼재 데이터로 CANCELLED 제외 실증) | ADMIN 역할만 조회 가능(@PreAuthorize), 비ADMIN 403 실제 확인,
회원 목록·상세 조회(#261): AdminMemberControllerTest로 토큰 없음 401·일반 회원 403 검증, 2026-09-28 member-service ./gradlew test BUILD SUCCESSFUL. | PR #187(Task 18-1, Closes #152), #189(Task 18-2, Closes #153) 둘 다 MERGED 
#263 | AdminSettlementController — API.md getSettlementDashboard 계약과 일치(startDate/endDate 필터 포함) | 9월 17일
Docker 전체 스택 + 실제 Gateway 경유로 ADMIN JWT 200, MEMBER JWT 403 직접 확인. 검증 중 Gateway에 /api/admin/settlements 라우팅 자체가 누락된 걸 발견해 별도 수정(feature/admin-settlements-gateway-route)
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/35182725644 | Reservation-Service 테스트 스위트 BUILD SUCCESSFUL, Docker curl 검증 로그
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/35056358444
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/35827772679 | DONE |
| Story 19 (#128) PG 연동 키 등록 | JWT 포팅(19-1) + PATCH /api/admin/settings/pg-key 등록·마스킹(19-2) + 인수테스트(19-3) + Gateway 라우팅(19-4) 전부 완료 | PgCredentialServiceTest(4개), PgCredentialAcceptanceTest(4개, 200/403/401/400) | secretKey AES/GCM 암호화 저장(평문 아님, DB 직접 조회로 확인), 응답은 뒤 4자리만 노출 | PR #164(19-1), #177(19-2), #178(19-3), #181(19-4) 전부 MERGED  | AdminPgCredentialController — API.md 계약과 일치 | 9월 17일
Docker 전체 스택 + 실제 Gateway 경유로 등록 200(마스킹 확인)·비ADMIN 403·토큰없음 401 직접 확인. 검증 중 .env.example/compose.yaml에 PG_CREDENTIAL_ENCRYPTION_KEY 배선 누락 발견해 별도 수정
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/35182725644 | Reservation-Service 테스트 스위트 BUILD SUCCESSFUL, Docker curl 검증 로그(DB 암호화 확인 포함)
https://github.com/likelion-backend-24th/Final-Project-Team3/actions/runs/34934021433 | DONE |

## 별도 품질평가와의 관계

Week 4의 `PASS/REWORK_REQUIRED`는 품질평가 담당자가 산출물 품질과 프로젝트 기준을 확인하는 별도 품질 Gate입니다. 별도 품질평가가 `PASS`여도 완료 기준을 만족하지 않은 PBI가 Done으로 바뀌지는 않으며, Sprint Review 또한 릴리스를 허가하는 Gate가 아닙니다.