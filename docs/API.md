# API·통합 계약

> **작성·동기화 메타정보**
> 
> 
> Notion 원본 URL: https://www.notion.so/API-3cd73873401a80bda31be290a53d6db4?source=copy_link
> 
> Snapshot 기준 시점: `Sprint 4 Review 종료 시점 (2026-09-28)`
> 
> 동기화 시각: `2026-09-29 11:00 KST`
> 
> 직접 편집 금지: Git Snapshot은 직접 편집하지 않고 Notion 원본을 수정한 뒤 다시 동기화합니다.
> 
> 관련 Story: Story 1~13, 15~24 전체(자세한 매핑은 각 절 참고) / 관련 업무 규칙: 요구사항 정의서 4절 전체
> 

## 공통 규약

| 항목 | 계약 |
| --- | --- |
| 인증 | Access Token(JWT, Bearer) — 로그인 후 `Authorization: Bearer <token>` 헤더로 전달. Role: `MEMBER`(참가자) / `ORGANIZER`(주최자) / `ADMIN`(전체관리자). Access Token 유효기간 30분, subject=memberId, claim `email`/`name`/`role`(+ORGANIZER는 `organizerId`·`organizationName`) |
| 성공 Envelope | `{ "success": true, "data": {...}, "message": "string", "meta": {...}?, "traceId": "string" }`(null 필드는 생략) |
| 실패 Envelope | `{ "success": false, "error": { "code": "string", "message": "string" }, "traceId": "string" }` |
| 목록 페이지네이션 | `meta.pagination = { page, size, totalItems, totalPages, hasNext, hasPrev }` |
| Trace ID | Gateway가 요청마다 `X-Trace-Id` 생성 후 전 서비스에 전파, 응답 Envelope에도 포함 |
| 시간·Timezone | ISO-8601, `Asia/Seoul`(KST) |
| 401 Header | `WWW-Authenticate: Bearer` |
| 내부 API | `/internal/**`(Reservation-Service)·`GET /api/conferences/{id}/session-ids`·`GET /api/sessions/{sessionId}/price`·`GET /api/sessions/{sessionId}/conference-id`·`GET /api/organizers/{id}/withdrawal-eligibility`(Conference-Service) 등은 Gateway 외부에 노출하지 않는 서비스 간 전용 경로이며 별도 인증 토큰 없이 호출됨(Swagger에서도 제외) |
| 권한 스코프 검증 | Role별 소유 자원 제한([소유자원-접근제한] 규칙)은 각 서비스가 `OwnerScopeGuard`(Conference-Service) 또는 서비스 메서드 내 소유자 비교(Reservation-Service)로 직접 구현. 스코프 밖 요청은 일관되게 `403` |
| Refresh Token | **HttpOnly 쿠키(`refreshToken`, path=`/api/auth`)로 전달** — 응답 Body에 포함하지 않음. 유효기간 14일. DB에는 해시(SHA-256)로 저장, 재발급마다 Rotation 적용. 이미 폐기된 토큰이 재사용되면 탈취로 간주해 해당 회원의 모든 Refresh Token을 무효화 |
| Validation 실패 | 전 서비스 공통 — `@Valid` 위반 시 `400 INVALID_REQUEST`, message는 `"{필드명}: {검증메시지}"` |
| AI 요약 LLM | 참석자 통계 요약(`getAttendeeSummary`)·소개글 요약(`applyConference`/`updateConferenceDescription`) 모두 **Gemini API**(Google Generative Language API, 멀티모달)를 사용한다. 과거 설계 문서의 "Claude API"는 실제 구현과 다르므로 이 Snapshot부터 Gemini로 정정 |

## 1. 이메일 인증 (Member-Service)

### `sendVerificationCode` — `POST /api/auth/email/send-code`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·규칙 | Story 2·3 / [이메일인증-필수] [인증코드-유효성] |
| Request | Body: `email` |
| 정상 | `200` — 6자리 인증코드 발급(SHA-256 해시 저장) 후 이메일 발송, 유효기간 10분. 재발송 시 기존 코드는 전량 삭제 후 재발급 |
| 실패 | 이미 가입된 이메일 `409`(`MEMBER_DUPLICATE_EMAIL`) / 최초 발급 후 1분 이내 재요청 `429`(`AUTH_EMAIL_CODE_RESEND_TOO_SOON`) |
| 보안 | 인증 불필요 |
| 상태 | DONE |

### `verifyEmailCode` — `POST /api/auth/email/verify`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·규칙 | Story 2·3 / [인증코드-유효성] |
| Request | Body: `email`, `code`(6자리) |
| 정상 | `200` — 코드 일치 시 해당 이메일을 인증완료 상태로 마킹(회원가입 전 단계) |
| 실패 | 발급 이력 없음·불일치 `400`(`AUTH_EMAIL_CODE_INVALID`), 만료 `400`(`AUTH_EMAIL_CODE_EXPIRED`), 5회 실패 `429`(`AUTH_EMAIL_CODE_ATTEMPTS_EXCEEDED`, 재발송 필요). 코드 불일치는 실패해도 시도횟수가 별도 트랜잭션으로 즉시 반영됨 |
| 보안 | 인증 불필요 |
| 상태 | DONE |

## 2. 회원가입·로그인·세션 (Member-Service)

### `signup` — `POST /api/members/signup`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·규칙 | Story 2 / [이메일-중복-금지] [프로필-연령대직무-필수] |
| Request | Body: `email`, `password`(8~72자), `name`, `ageGroup`, `job` |
| 정상 | `201` — 참가자(`MEMBER`) 계정 생성(BCrypt strength 12), 이메일 인증 레코드 소비(재사용 방지) |
| 실패 | 이메일 중복 `409`(`MEMBER_DUPLICATE_EMAIL`) / 이메일 미인증 `400`(`MEMBER_EMAIL_NOT_VERIFIED`) |
| 보안 | 인증 불필요 |
| 상태 | DONE |

### `signupOrganizer` — `POST /api/members/organizers/signup`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·규칙 | Story 3 / [사업자번호-인증필수] [주최자가입-중복금지] [이메일인증-필수] |
| Request | Body: `email`, `password`, `name`, `organizationName`, `businessNo`(10자리 숫자) |
| 정상 | `201` — 사업자등록번호 형식 검증(정규식) + 국세청(NTS) 상태조회 API로 실재·영업중 여부 확인 통과 시 즉시 `ORGANIZER` 계정 생성(관리자 승인 없음). `NTS_VERIFY_MODE=mock`이면 형식 검증만 수행하는 Mock으로 대체 |
| 실패 | 형식 오류 `400`(`MEMBER_INVALID_BUSINESS_NO`) / 미등록 사업자번호 `400`(`MEMBER_BUSINESS_NOT_REGISTERED`) / 휴폐업 사업자 `400`(`MEMBER_BUSINESS_NOT_ACTIVE`) / NTS 응답 실패·Timeout `503`(`MEMBER_BUSINESS_VERIFY_UNAVAILABLE`) / 이메일 중복 `409`(`MEMBER_DUPLICATE_EMAIL`) / 사업자번호 중복 `409`(`MEMBER_DUPLICATE_BUSINESS_NO`) / 이메일 미인증 `400`(`MEMBER_EMAIL_NOT_VERIFIED`) |
| 보안 | 인증 불필요 |
| 상태 | DONE |

### `login` — `POST /api/auth/login`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story | Story 2·4 |
| Request | Body: `email`, `password` |
| 정상 | `200` — Access Token(Body) + Refresh Token(HttpOnly 쿠키) 발급. `ORGANIZER`는 `organizerId`·`organizationName` Claim 추가 |
| 실패 | 회원 없음·비밀번호 불일치·소셜 전용 계정(비밀번호 없음) 모두 동일하게 `401`(`AUTH_INVALID_CREDENTIALS`, 이메일 존재 여부 비노출) |
| 보안 | 인증 불필요 |
| 상태 | DONE |

### `socialLogin` — `POST /api/auth/social/{provider}`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·규칙 | Story 22 / [소셜로그인-참석자전용] [카카오-이메일대체] [소셜계정-수동연동] |
| Request | Path: `provider`(`google`\|`kakao`) / Body: `token`(Google access token 또는 Kakao 인가 코드), `ageGroup`·`job`(최초 가입 시 필수), `redirectUri`(Kakao만) |
| 정상 | `200` — Provider 토큰 검증(Google: tokeninfo+userinfo, `email_verified` 필수 / Kakao: 인가코드→액세스토큰 교환 후 `/v2/user/me`, 이메일 미동의 시 `kakao_{id}@kakao.local` 대체) 후 기존 연동 계정이면 로그인, 최초면 `role=MEMBER` 계정+`social_account` 신규 생성. 로그인 응답 형식은 `login`과 동일(쿠키+Body) |
| 실패 | 지원하지 않는 provider `400`(`AUTH_SOCIAL_PROVIDER_UNSUPPORTED`) / 토큰 검증 실패 `401`(`AUTH_SOCIAL_TOKEN_INVALID`) / 이미 `ORGANIZER`·`ADMIN`으로 가입된 이메일 `409`(`AUTH_SOCIAL_EMAIL_NOT_LINKABLE`) / 이미 `MEMBER`로 가입된 이메일(자동연동 안 함) `409`(`AUTH_SOCIAL_EMAIL_ALREADY_REGISTERED`, 비밀번호 로그인 후 별도 연동 필요) / 최초 가입인데 `ageGroup`/`job` 누락 `400`(`AUTH_SOCIAL_PROFILE_REQUIRED`) |
| 보안 | 인증 불필요. 이 경로로는 `ORGANIZER`/`ADMIN`이 절대 발급되지 않음(코드 레벨로 고정) |
| 상태 | DONE |

### `linkSocialAccount` — `POST /api/auth/social/{provider}/link`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·규칙 | Story 22 / [소셜계정-수동연동] |
| Request | Path: `provider` / Body: `token`, `redirectUri`(선택) |
| 정상 | `200` — 로그인 중인 계정에 소셜 계정 연동(`social_account` 추가) |
| 실패 | 검증된 소셜 이메일이 로그인 계정 이메일과 다름 `400`(`AUTH_SOCIAL_LINK_EMAIL_MISMATCH`) / 이미 다른 계정에 연동된 소셜 계정 `409`(`AUTH_SOCIAL_ACCOUNT_ALREADY_LINKED`) |
| 보안 | Role 무관, 로그인 필요(Access Token) |
| 상태 | DONE |

### `refresh` — `POST /api/auth/refresh`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story | Story 2 |
| Request | Cookie: `refreshToken` |
| 정상 | `200` — 새 Access Token + Refresh Token 쌍 발급(Rotation: 기존 토큰 즉시 `revoked=true`) |
| 실패 | 쿠키 없음/빈 값 `401`(`AUTH_REFRESH_TOKEN_MISSING`) / 존재하지 않음 `401`(`AUTH_REFRESH_TOKEN_NOT_FOUND`) / 만료 `401`(`AUTH_REFRESH_TOKEN_EXPIRED`) / 이미 폐기(rotate)된 토큰 재사용 시 해당 회원의 모든 Refresh Token 즉시 무효화 후 `401`(`AUTH_REFRESH_TOKEN_REUSED`) |
| 보안 | 인증 불필요 — Refresh Token 자체가 인증 수단 |
| 상태 | DONE |

### `logout` — `POST /api/auth/logout`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| Request | Cookie: `refreshToken`(선택) |
| 정상 | `200` — 항상 성공(멱등). 쿠키가 있으면 해당 토큰만 폐기(전체 무효화 아님), 만료 쿠키로 교체 |
| 보안 | 인증 불필요 |
| 상태 | DONE |

### `resetPasswordRequest` — `POST /api/auth/password/reset-request`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·규칙 | Story 23 / [비밀번호재설정-코드유효성] [소셜전용계정-재설정불가] |
| Request | Body: `email` |
| 정상 | `200` — 6자리 인증코드 발급(SHA-256 해시, 유효 10분), 이메일 발송 |
| 실패 | 가입되지 않은 이메일 `404`(`MEMBER_NOT_FOUND`, 존재 여부가 이 API에서는 노출됨) / 소셜 전용 계정(비밀번호 없음) `409`(`AUTH_PASSWORD_RESET_SOCIAL_ONLY_ACCOUNT`) / 1분 이내 재요청 `429`(`AUTH_PASSWORD_RESET_RESEND_TOO_SOON`) |
| 보안 | 인증 불필요 |
| 상태 | DONE |

### `resetPasswordConfirm` — `POST /api/auth/password/reset-confirm`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·규칙 | Story 23 / [비밀번호재설정-코드유효성] [재설정-세션전체무효화] |
| Request | Body: `email`, `code`(6자리), `newPassword`(8~72자) |
| 정상 | `200` — 비밀번호 변경(BCrypt) + 해당 회원의 **모든 Refresh Token 무효화**(전 기기 로그아웃) |
| 실패 | 코드 없음·불일치 `400`(`AUTH_PASSWORD_RESET_CODE_INVALID`) / 만료 `400`(`AUTH_PASSWORD_RESET_CODE_EXPIRED`) / 5회 실패 `429`(`AUTH_PASSWORD_RESET_ATTEMPTS_EXCEEDED`) |
| 보안 | 인증 불필요 |
| 상태 | DONE |

## 3. 마이페이지·회원 탈퇴 (Member-Service)

### `getMyProfile` — `GET /api/members/me`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 정상 | `200` — `{memberId, email, name, ageGroup, job, hasPassword, organizationName, businessNo}`. `hasPassword=false`면 소셜 전용 계정 |
| 보안 | 로그인 필요, Role 무관 |
| 상태 | DONE |

### `updateProfile` — `PATCH /api/members/me`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·규칙 | Story 2 / [프로필-정보-수정가능] |
| Request | Body: `ageGroup`, `job`(둘 다 필수) |
| 정상 | `200` — 연령대·직무 수정. 이메일·이름·소속·사업자번호는 이 API로도, 다른 어떤 API로도 변경할 수 없음(변경 엔드포인트 자체가 없음). 세션 신청 화면(`SessionApply`)이 이 프로필 값을 조회해 본인 좌석의 연령대·직무를 자동 채워주는 데도 쓰인다 |
| 보안 | 로그인 필요, Role 무관 |
| 상태 | DONE |

### `getLinkedSocialAccounts` — `GET /api/members/me/social-accounts`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 정상 | `200` — `[{provider, linkedAt}]` |
| 보안 | 로그인 필요 |
| 상태 | DONE |

### `withdraw` — `DELETE /api/members/me`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·규칙 | Story 24 / [주최자탈퇴-진행중컨퍼런스금지] [탈퇴-즉시익명화] |
| Request | Body: 비밀번호 계정은 `password`, 소셜 전용 계정은 `provider`+`socialToken` |
| 정상 | `200` — 본인확인 성공 시 `email`/`password`/`name` NULL 처리 + `withdrawn=true`(row는 보존), 모든 Refresh Token 무효화. 같은 이메일로 즉시 재가입 가능 |
| 실패 | 비밀번호 불일치 `401`(`MEMBER_WITHDRAWAL_PASSWORD_MISMATCH`) / 소셜 신원 불일치 `401`(`MEMBER_WITHDRAWAL_SOCIAL_IDENTITY_MISMATCH`) / `ORGANIZER`가 진행 중(`PENDING`·`APPROVED`) 컨퍼런스 보유 `409`(`MEMBER_ORGANIZER_WITHDRAWAL_BLOCKED`) / Conference-Service 조회 실패 `503`(`MEMBER_WITHDRAWAL_ELIGIBILITY_CHECK_FAILED`) |
| 보안 | 로그인 필요, Role 무관(주최자는 추가로 Conference-Service `withdrawal-eligibility` 동기 조회) |
| 상태 | DONE |

## 4. 관리자 회원 조회 (Member-Service, 전체 `ADMIN` 전용)

| operationId | Method/Path | 정상 | 실패 |
| --- | --- | --- | --- |
| `listMembers` | `GET /api/admin/members` | `200` 목록(+`meta.pagination`), Query: `keyword`(이메일·이름 LIKE), `page`,`size`,`sort` | `403` `ADMIN` 아님 |
| `getMemberDetail` | `GET /api/admin/members/{memberId}` | `200` 상세 | `404` `MEMBER_NOT_FOUND` / `403` |
| `changeMemberRole` | `PATCH /api/admin/members/{memberId}/role` | `200` — `MEMBER`↔`ORGANIZER`↔`ADMIN` 어떤 방향으로도 변경 허용(전이 제한 없음) | `404` / `403` |

상태: 전체 DONE. 관련 Story 18(#150 정산 대시보드의 Parent Story 하위로 생성된 관리자 회원조회 Task 18-4·18-5), 시나리오 19 / [회원조회-관리자전용]. `@PreAuthorize`가 정상 작동하려면 `@EnableMethodSecurity`가 필요한데 최초 구현 시 빠져 있어 미인증 요청이 403이 아니라 다른 방식으로 새어나갔던 결함이 있었고, Task 18-4에서 함께 수정됨(`AccessDeniedException`→`403` 매핑 포함).

## 5. 컨퍼런스 조회·등록 (Conference-Service)

### `listConferences` — `GET /api/conferences`

| 항목 | 정의 |
| --- | --- |
| Request | Query: `keyword`, `page`, `size` |
| 정상 | `200` — 상태=`APPROVED`인 컨퍼런스만 필터되어 반환(+`meta.pagination`) |
| 보안 | 인증 불필요 |
| 상태 | DONE |

### `getMyConferences` — `GET /api/conferences/my`

| 항목 | 정의 |
| --- | --- |
| 정상 | `200` — 요청 주최자가 등록한 전체 상태(PENDING/APPROVED/REJECTED)의 컨퍼런스 목록 |
| 보안 | Role `ORGANIZER` |
| 상태 | DONE |

### `getConference` — `GET /api/conferences/{id}`

| 항목 | 정의 |
| --- | --- |
| 정상 | `200` — 상태=`APPROVED`인 컨퍼런스 상세(세션 목록·태그·주최자 과거 컨퍼런스 수·대표 후기 요약 포함) |
| 실패 | 미승인·존재하지 않는 컨퍼런스는 동일하게 `404`([승인-전-비공개] 규칙, 추측 방지) |
| 보안 | 인증 불필요 |
| 상태 | DONE |

### `applyConference` — `POST /api/conferences` (multipart/form-data)

| 항목 | 정의 |
| --- | --- |
| 관련 Story·규칙 | Story 5·26 / [소개글-이미지-1장] [소개글AI요약-저장독립] |
| Request | `title`, `capacity`(≥1), `startAt`(미래시각), `endAt`, `location`, `transportation`, `parkingInfo`, `amenities`, `description`, `tags`(1개 이상), 썸네일/상세 이미지 파일, 사업자 증빙파일(선택) |
| 정상 | `201` — 상태=`PENDING` 저장(주최기관명은 JWT `organizationName` Claim으로 고정, 자유입력 아님). 저장 성공 후 Gemini로 `aiSummary` 자동 생성(실패해도 등록 자체는 성공) |
| 실패 | `organizationName` 없음 `401`(`ORGANIZATION_NAME_NOT_FOUND`) / `endAt <= startAt` `400`(`INVALID_CONFERENCE_PERIOD`) / 소개글 이미지 2장 이상 `400`(`DESCRIPTION_IMAGE_LIMIT_EXCEEDED`) / 태그 누락·기타 필수값 누락 `400` / 이미지 형식·용량 위반 `400`(`IMAGE_INVALID_TYPE`/`IMAGE_DIMENSIONS_TOO_LARGE`) |
| 보안 | Role `ORGANIZER` |
| 상태 | DONE |

### `updateConferenceDescription` — `PATCH /api/conferences/{id}/description`

| 항목 | 정의 |
| --- | --- |
| Request | Body: `description` |
| 정상 | `200` — 소개글 수정, 승인 여부와 무관하게 항상 가능. 저장 후 Gemini로 `aiSummary` 재생성(실패해도 저장은 성공) |
| 실패 | 소유자 아님 `403`(`CONFERENCE_ACCESS_DENIED`) / 존재하지 않음 `404` |
| 보안 | Role `ORGANIZER` + 소유자 |
| 상태 | DONE |

### `updateConferenceLocation` — `PATCH /api/conferences/{id}/location`

| 항목 | 정의 |
| --- | --- |
| Request | Body: `location`, `transportation`, `parkingInfo`, `amenities` |
| 정상 | `200` — 네 필드 모두 수정. `location` 값이 기존과 같으면(주소 변경 아님) 나머지 필드만 바뀌어도 정상 처리 |
| 실패 | `status=APPROVED`인데 `location` 값이 실제로 바뀌면 `409`(`CONFERENCE_LOCATION_ADDRESS_LOCKED`, [장소-수정불가]) / 소유자 아님 `403` / 없음 `404` |
| 보안 | Role `ORGANIZER` + 소유자 |
| 상태 | DONE |

### 이미지·증빙파일

| operationId | Method/Path | 설명 | 보안 |
| --- | --- | --- | --- |
| `uploadDescriptionImage` | `POST /api/conferences/description-images` | 소개글 본문 삽입용 이미지 업로드(`201`), 확장자 png/jpg/jpeg만 허용 | `ORGANIZER` |
| `getConferenceImage` | `GET /api/conferences/images/{filename}` | 썸네일·상세 이미지 서빙, `Cache-Control: public, max-age=31536000, immutable` | 불필요 |
| `getProofFile` | `GET /api/conferences/{id}/proof-file` | 사업자 증빙파일 열람 | `ORGANIZER`+소유자 또는 `ADMIN`(ADMIN은 소유자 검증 생략) |

실패 공통: 형식 위반 `400`(`PROOF_FILE_INVALID_TYPE`/`IMAGE_INVALID_TYPE`), 없음 `404`, 업로드 실패 `500`. 상태: 전체 DONE.

## 6. 세션 CRUD (Conference-Service)

### `createSession` — `POST /api/conferences/{conferenceId}/sessions`

| 항목 | 정의 |
| --- | --- |
| Request | `title`, `capacity`(>0), `startAt`/`endAt`(신청 기간, 미래), `sessionStartAt`/`sessionEndAt`(진행 일정), `location`, `speaker`, `price`(≥0), `maxHeadcountPerApplication`(>0) |
| 정상 | `201` — 세션 생성(기본 상태 `PENDING`), 정원·기간이 참가자 신청 화면(`getSessionCapacity` 등)에 즉시 반영 |
| 실패 | `capacity≤0`·`endAt≤startAt` `400`(`INVALID_SESSION_CAPACITY`/`INVALID_SESSION_PERIOD`, [세션정원-유효성]) / `sessionEndAt≤sessionStartAt` `400`(`INVALID_SESSION_SCHEDULE`) / `sessionStartAt < endAt`(신청기간 종료 전 진행) `400`(`INVALID_SESSION_SCHEDULE_BEFORE_REGISTRATION`) / `maxHeadcountPerApplication > capacity` `400`(`MAX_HEADCOUNT_EXCEEDS_CAPACITY`) / 컨퍼런스가 `APPROVED` 아님 `409`(`CONFERENCE_NOT_APPROVED`) / 세션 진행일정이 컨퍼런스 기간 밖 `400`(`SESSION_SCHEDULE_OUTSIDE_CONFERENCE_PERIOD`) / 세션 정원이 컨퍼런스 정원 초과 `409`(`SESSION_CAPACITY_EXCEEDS_CONFERENCE_CAPACITY`) / 소유자 아님 `403`(`SESSION_ACCESS_DENIED`) |
| 보안 | Role `ORGANIZER` + 컨퍼런스 소유자 |
| 상태 | DONE |

### `updateSession` — `PATCH /api/sessions/{sessionId}`

| 항목 | 정의 |
| --- | --- |
| 정상 | `200` — 정원·일정·장소·연사·가격 등 수정. 승인/반려된 세션의 값이 실제로 바뀌면(또는 기존 REJECTED였으면) 상태가 자동으로 `PENDING`으로 재전이(재승인 필요) |
| 실패 | 위 생성 규칙과 동일 + 확정 예약이 있는 세션의 정원을 확정 인원 미만으로 낮추면 `409`(`SESSION_CAPACITY_BELOW_CONFIRMED_COUNT`) / 확정 예약이 있는데 일정을 바꾸면 `409`(`SESSION_SCHEDULE_CHANGE_WITH_ACTIVE_RESERVATIONS`) / Reservation-Service 확정 인원 조회 실패 `503`(`SESSION_RESERVATION_SERVICE_UNAVAILABLE`) / 소유자 아님 `403` |
| 보안 | Role `ORGANIZER` + 소유자 |
| 상태 | DONE |

### 세션 조회

| operationId | Method/Path | 설명 | 보안 |
| --- | --- | --- | --- |
| `listSessionsByConference` | `GET /api/conferences/{conferenceId}/sessions` | 소유 컨퍼런스의 전체 상태 세션 목록 | `ORGANIZER`+소유자 |
| `getSessionCapacity` | `GET /api/sessions/{sessionId}/capacity` | `APPROVED` 컨퍼런스 소속 세션만 공개 조회(신청 화면용) | 불필요 |
| `getSessionStartAt` | `GET /api/sessions/{sessionId}/startat` | 세션 진행 시작 시각(취소 환불율·QR 스캔 조기입장 방지 계산용) | 불필요(내부 겸용) |
| `getSessionPrice`(내부) | `GET /api/sessions/{sessionId}/price` | 1인당 가격(결제·환불 금액 계산용) | 내부 |
| `getConferenceIdBySession`(내부) | `GET /api/sessions/{sessionId}/conference-id` | | 내부 |

상태: 전체 DONE.

## 7. 관리자 승인·반려 — 컨퍼런스·세션 (Conference-Service, 전체 `ADMIN`)

| operationId | Method/Path | 정상 | 실패 |
| --- | --- | --- | --- |
| `listPendingConferences` | `GET /api/admin/conferences` | `200` `PENDING` 목록 | `403` |
| `getAdminConferenceDetail` | `GET /api/admin/conferences/{id}` | `200` 상태 무관 상세(세션 전체 포함) | `404` |
| `approveConference` | `PATCH /api/admin/conferences/{id}/approve` | `200` `PENDING→APPROVED` | 이미 결정됨 `409`(`CONFERENCE_ALREADY_DECIDED`) |
| `rejectConference` | `PATCH /api/admin/conferences/{id}/reject` | `200` `PENDING→REJECTED`+사유 저장 | `reason` 누락 `400`, 이미 결정됨 `409` |
| `listPendingSessions` | `GET /api/admin/sessions` | `200` `PENDING` 목록 | `403` |
| `approveSession` | `PATCH /api/admin/sessions/{id}/approve` | `200` `PENDING→APPROVED` | 이미 결정됨 `409`(`SESSION_ALREADY_DECIDED`) |
| `rejectSession` | `PATCH /api/admin/sessions/{id}/reject` | `200` `PENDING→REJECTED`+사유 저장 | `reason` 누락 `400`, 이미 결정됨 `409` |

상태: 전체 DONE.

## 8. 공지사항·FAQ (Conference-Service)

동일한 CRUD 패턴을 공지사항(`/api/conferences/{conferenceId}/notices`)과 FAQ(`/api/conferences/{conferenceId}/faqs`)에 각각 적용.

| 동작 | Method/Path | Request | 정상 | 실패 | 보안 |
| --- | --- | --- | --- | --- | --- |
| 목록 조회 | `GET .../notices`, `GET .../faqs` | - | `200` 목록(최신순), 빈 목록도 `200` | 없음 | 불필요 |
| 등록 | `POST .../notices`, `POST .../faqs` | 공지: `title`,`content` / FAQ: `question`,`answer` | `201` | 소유자 아님 `403`(`NOTICE_ACCESS_DENIED`/`FAQ_ACCESS_DENIED`) / 필수값 누락 `400` | `ORGANIZER`+소유자 |
| 수정 | `PATCH .../notices/{noticeId}`, `PATCH .../faqs/{faqId}` | 동일 필드 | `200`, `updatedAt` 갱신 | 소유자 아님 `403` / 없음 `404` | `ORGANIZER`+소유자 |
| 삭제 | `DELETE .../notices/{noticeId}`, `DELETE .../faqs/{faqId}` | - | `204` | 소유자 아님 `403` / 없음 `404` | `ORGANIZER`+소유자 |

상태: 전체 DONE.

## 9. 주최자 프로필·운영·통계·정산 조회 (Conference-Service, `ORGANIZER`+소유자 전용 — 프로필 조회 제외)

### `getOrganizerProfile` — `GET /api/organizers/{organizerId}/profile`

| 항목 | 정의 |
| --- | --- |
| 관련 Story | Story 21 |
| 정상 | `200` — 해당 주최자의 승인(`APPROVED`) 컨퍼런스만 종료 여부로 `pastConferences`/`ongoingConferences` 분류해 반환. 컨퍼런스가 하나도 없어도 `404`가 아니라 빈 목록으로 `200` |
| 보안 | 인증 불필요 |
| 상태 | DONE |

### `getAttendeeSummary` — `GET /api/conferences/{conferenceId}/attendee-summary`

| 항목 | 정의 |
| --- | --- |
| 관련 Story·규칙 | Story 15 / [AI요약-실참석기준] |
| 정상 | `200` — 체크인 완료(QR 사용) 좌석만 집계한 연령대·직무 분포 + Gemini 요약 문장. `checkedInCount`가 캐시(`conference_attendee_summary`)와 동일하면 재계산 없이 캐시 반환. `checkedInCount=0`이면 "아직 체크인한 참석자가 없습니다." 고정 문구(LLM 미호출) |
| 실패 | 소유자 아님 `403`(`CONFERENCE_ACCESS_DENIED`) / Reservation-Service 집계 조회 실패 `503`(`RESERVATION_SERVICE_UNAVAILABLE`) / Gemini 호출 실패 시에도 요청 자체는 `200`이며 `summaryText`만 "요약 정보를 일시적으로 생성하지 못했습니다."로 대체 |
| 보안 | `ORGANIZER`+소유자 |
| 상태 | DONE |

### `getConferenceReviews` — `GET /api/conferences/{conferenceId}/reviews`

| 항목 | 정의 |
| --- | --- |
| 정상 | `200` — 후기 원문 목록(`List<String>`, 작성자 식별정보 없음, [후기-익명]) |
| 보안 | `ORGANIZER`+소유자 |
| 상태 | DONE |

### `getOperationStatus` — `GET /api/conferences/{conferenceId}/operation-status`

| 항목 | 정의 |
| --- | --- |
| 관련 Story | Story 16 |
| 정상 | `200` — 세션별(`sessionStartAt` 오름차순) `holdCount`/`queuedCount`/`confirmedCount`/`cancelledCount`/`checkedInCount` |
| 실패 | 소유자 아님 `403` / Reservation-Service 세션별 집계 조회 실패 시 즉시 `503`(`RESERVATION_SERVICE_UNAVAILABLE`, 일부 세션만 성공하는 부분 응답 없음) |
| 보안 | `ORGANIZER`+소유자 |
| 상태 | DONE |

### `getSettlement` — `GET /api/conferences/{conferenceId}/settlement`

| 항목 | 정의 |
| --- | --- |
| 관련 Story | Story 17 |
| 정상 | `200` — `{conferenceId, conferenceTitle, totalRevenue, refundedAmount, netRevenue, confirmedCount, cancelledCount}` |
| 실패 | 소유자 아님 `403` / Reservation-Service 결제 집계 조회 실패 `503`(`RESERVATION_SERVICE_UNAVAILABLE`) |
| 보안 | `ORGANIZER`+소유자 |
| 상태 | DONE |

## 10. 세션 신청·대기열 (Reservation-Service)

### `createHold` — `POST /api/reservations/hold`

| 항목 | 정의 |
| --- | --- |
| 관련 Story·규칙 | Story 9·10 / [정원-초과-금지] [동반자정보-필수입력] [대기열-순번-제한] |
| Request | Body: `sessionId`, `headcount`, `attendees`(9명 이하, 개별 `{ageGroup,job}` 배열 — 화면이 본인(1번) 좌석만 로그인 회원 프로필값으로 자동 채워 보내지만, 계약상 여전히 클라이언트가 값을 명시해야 함), `groupAttendee`(10명 이상, 전원 동일값 1개) |
| 정상 | 정원 내: `201` `{reservationId, status:"HOLD"}`, `expiresAt`=생성시각+10분(HOLD는 10분 내 결제 필요, 만료 시 자동취소) / 정원 초과: `409` `{reservationId, status:"QUEUED", queuePosition}`(별도 API 없이 자동 대기열 등록) |
| 실패 | 동반자 정보 미입력·개수 불일치 `400`(`ATTENDEE_INFO_REQUIRED`) / 동일 세션에 이미 활성(HOLD/QUEUED/CONFIRMED) 예약 존재 `409`(`RESERVATION_DUPLICATE`) / `headcount`가 세션 정원 초과 `409`(`SESSION_CAPACITY_EXCEEDED`) / Conference-Service 정원 조회 실패 `503`(`CONFERENCE_SERVICE_UNAVAILABLE`, 홀드 저장 안 함) |
| 보안 | Role `MEMBER` |
| 상태 | DONE |

### `getQueuePosition` — `GET /api/reservations/{id}/queue-position`

| 항목 | 정의 |
| --- | --- |
| 정상 | `200` — `{position, estimatedWaitMinutes}`. `estimatedWaitMinutes`는 "평균 결제 소요시간 × 순번"이 아니라 **결제 대기(HOLD) 좌석의 만료 시각 기준**으로 계산한다([대기열-예상시간-추정] 산출 방식 확정, 2026-09-28): 이미 여유 좌석이 내 순번까지 충분하면 최소값(다음 승격 스케줄러 주기, 1분), 부족하면 만료가 빠른 HOLD부터 필요 인원만큼 누적해 그 마지막 HOLD의 만료 시각까지 남은 시간(분)을 반환. 좌석 대부분이 이미 결제 완료라 HOLD만으로 부족분을 채울 수 없으면 `estimatedWaitMinutes: null`(취소가 나와야만 자리가 나는 상태 — 시간 예측 불가) |
| 실패 | 대기열에 없음 `404`(`RESERVATION_NOT_IN_QUEUE`) / 본인 소유 아님 `403`(`RESERVATION_ACCESS_DENIED`) |
| 보안 | Role `MEMBER`, 본인 소유만 |
| 상태 | DONE |

### 세션 현황 조회(공개)

| operationId | Method/Path | 설명 | 보안 |
| --- | --- | --- | --- |
| `getCapacityStatus` | `GET /api/reservations/sessions/{sessionId}/capacity-status` | `{capacity, confirmedCount, remaining}` | 불필요 |
| `getStatusSummary` | `GET /api/reservations/sessions/{sessionId}/status-summary` | `{holdCount, queuedCount, confirmedCount, cancelledCount, checkedInCount}` — `/internal/sessions/{id}/status-summary`와 동일 로직을 인증 경로로도 노출 | 인증 필요(Role 무관) |

상태: 전체 DONE.

## 11. 결제 — PortOne 연동 (Reservation-Service)

### `processPayment` — `POST /api/reservations/{id}/payment`

| 항목 | 정의 |
| --- | --- |
| 관련 Story | Story 11 |
| Request | Path: `id`(reservationId) / Body: `paymentId`(PortOne 결제 ID — 프론트가 PortOne SDK 결제창 완료 후 전달, 관례상 `paymentId == reservationId` 문자열) |
| 정상 | `200` — 세션 가격 조회(`price×headcount`)를 기대금액으로 삼아 **PortOne 서버에 결제내역을 직접 재조회해 금액·상태를 검증**(클라이언트 신고값을 신뢰하지 않음). 검증 통과 후 조건부 UPDATE로 좌석 확정, 대기열이었다면 대기열에서 제거+뒷사람 순번 당김(+`queue_position_counter` 감소), `Attendee` 수만큼 QR 티켓 발급. `price=0`이면 PortOne 호출 없이 무료 확정(`paymentMethod="FREE"`) |
| 실패 | 대기열 순번 미도달 `403`(`RESERVATION_QUEUE_POSITION_NOT_REACHED`) / 이미 확정된 건 재요청(웹훅과의 경쟁 포함) `409`(`RESERVATION_ALREADY_CONFIRMED`) / 검증 직후 정원이 소진되어 재확인 실패 `409`(`SESSION_CAPACITY_EXCEEDED`, 이 경우 PortOne 결제를 자동 취소·환불 처리) / PortOne 결제상태가 `PAID`가 아님 `402`(`RESERVATION_PAYMENT_NOT_PAID`) / PortOne 신고금액 불일치 `409`(`PAYMENT_AMOUNT_MISMATCH`) / PortOne에 결제 없음 `404`(`RESERVATION_PAYMENT_NOT_FOUND`) / PortOne API 오류 `503`(`PORTONE_API_ERROR`) / PG 미등록 `503`(`RESERVATION_PG_NOT_CONFIGURED`) / Conference-Service 가격 조회 실패 `503`(`CONFERENCE_SERVICE_UNAVAILABLE`) |
| 보안 | Role `MEMBER`, 본인 소유만(`RESERVATION_ACCESS_DENIED` 403) |
| 상태 | DONE |

### `paymentWebhook` — `POST /api/payments/webhook`

| 항목 | 정의 |
| --- | --- |
| Request | Header: `webhook-id`, `webhook-signature`, `webhook-timestamp` / Body: PortOne 원문(서명 검증에만 사용, 페이로드 자체는 신뢰하지 않음) |
| 정상 | `200` — 서명 검증 통과 시 `paymentId`(=reservationId)를 추출해 `processPayment`와 동일한 서버측 재검증 로직으로 좌석 확정(이미 확정된 건은 조용히 무시해 멱등 처리) |
| 실패 | 필수 헤더 누락 `400` / 서명 검증 실패 `401`(`RESERVATION_WEBHOOK_SIGNATURE_INVALID`) / PG 미등록·웹훅 시크릿 없음 `503`(`RESERVATION_PG_NOT_CONFIGURED`) |
| 보안 | 인증 불필요(PortOne 서버 전용 콜백, 서명으로 진위 확인) |
| 상태 | DONE |

### `getPgConfig` — `GET /api/payments/pg-config`

| 항목 | 정의 |
| --- | --- |
| 정상 | `200` — `{provider, storeId, channelKey}`(프론트 PortOne SDK 초기화용, `apiSecret`/`webhookSecret`은 응답에서 제외) |
| 실패 | PG 미등록 `404`(`RESERVATION_PG_CONFIG_NOT_FOUND`) |
| 보안 | 로그인 필요(Role 무관, ADMIN 아니어도 조회 가능) |
| 상태 | DONE |

## 12. QR 티켓·체크인 (Reservation-Service)

### `getQrTickets` — `GET /api/qr-tickets/{reservationId}`

| 항목 | 정의 |
| --- | --- |
| 정상 | `200` — 발급된 QR 티켓 전체 목록(연령대·직무·체크인 여부 포함) |
| 실패 | 예약 없음 `404`(`RESERVATION_NOT_FOUND`) / 결제 미완료 `404`(`PAYMENT_NOT_COMPLETED`) |
| 보안 | 로그인 필요(Role 무관) — **예약 소유자 검증 없음**(reservationId만 알면 다른 회원의 티켓도 조회 가능한 상태, 후속 보완 필요 항목으로 기록) |
| 상태 | DONE(보안 갭 있음) |

### `scanQrTicket` — `POST /api/qr-tickets/{code}/scan`

| 항목 | 정의 |
| --- | --- |
| 관련 Story·규칙 | Story 13 / [qr-1회성] |
| Request | Path: `code` |
| 정상 | `200` — `used=true`, `usedAt` 기록, 입장 처리 완료 |
| 실패 | 존재하지 않는 code `404`(`QR_TICKET_NOT_FOUND`) / 이미 사용됨 `409`(`QR_TICKET_ALREADY_USED`) / **세션 시작 전 스캔 시도** `403`(`QR_TICKET_SESSION_NOT_STARTED`, 조기 입장 방지) |
| 보안 | Role `ORGANIZER`(입장 담당자가 주최자 계정으로 스캔) |
| 상태 | DONE |

## 13. 취소·환불 — 전체·개별 (Reservation-Service)

### `cancelReservation` — `POST /api/reservations/{id}/cancel`

| 항목 | 정의 |
| --- | --- |
| 관련 Story·규칙 | Story 12 / [취소환불-기한] |
| 정상 | `200` — `{status:"CANCELLED", refundRate, refundAmount}`(HOLD/QUEUED는 결제한 적 없어 `refundRate`/`refundAmount`가 `null`). `CONFIRMED`는 세션 시작까지 남은 일수로 환불율(7일 이상 100% / 3~6일 50% / 3일 미만 0%) 계산 후 PortOne로 실제 환불 호출, 좌석 반환 + 대기열 즉시 승격(취소 직후 트리거, 스케줄러를 기다리지 않음). `QUEUED`는 대기열에서 제거하고 뒷사람 순번+`queue_position_counter`를 함께 당김 |
| 실패 | 이미 취소됨 `409`(`RESERVATION_ALREADY_CANCELLED`) / 본인 소유 아님 `403` |
| 보안 | Role `MEMBER`, 본인 소유만 |
| 상태 | DONE |

### `cancelTicket` — `POST /api/reservations/{id}/tickets/{ticketId}/cancel`

| 항목 | 정의 |
| --- | --- |
| 관련 Story·규칙 | Story 12(개별 확장, Task 12-5) / [취소환불-기한] |
| Request | Path: `reservationId`, `ticketId`(QrTicket ID — 한 예약에 묶인 동반자 중 1명 식별 기준) |
| 정상 | `200` — `{ticketId, refundRate, refundAmount, remainingHeadcount}`. 1인당 세션 가격 기준으로 전체 취소와 동일한 환불율 구간 적용, PortOne 부분 환불 호출, 해당 QR 티켓만 삭제하고 `headcount` 1 감소, 좌석 1석 반환 + 대기열 즉시 승격. 예약 상태는 `CONFIRMED`를 유지(전체 취소 아님) |
| 실패 | 예약이 `CONFIRMED`가 아님 `409`(`RESERVATION_NOT_CONFIRMED`) / 티켓 없음 `404`(`QR_TICKET_NOT_FOUND`) / 이미 체크인(사용)된 티켓 `409`(`QR_TICKET_ALREADY_USED`, 체크인한 인원은 개별 취소 불가) / **남은 유효 티켓이 1장뿐** `409`(`LAST_TICKET_CANNOT_BE_CANCELLED_INDIVIDUALLY`, 전체 취소 API 안내) / 본인 소유 아님 `403` |
| 보안 | Role `MEMBER`, 본인 소유만 |
| 상태 | DONE |

## 14. 후기 작성 (Reservation-Service)

### `writeReview` — `POST /api/reservations/{reservationId}/reviews`

| 항목 | 정의 |
| --- | --- |
| 관련 Story·규칙 | Story 20 / [후기-작성자격] [후기-1회성] [후기-익명] |
| Request | Body: `content` |
| 정상 | `200` — 예약(reservation) 단위 1건, 이미 있으면 내용 수정(update-or-insert, 별도 수정 API 없음) |
| 실패 | 예약 없음 `404`(`REVIEW_RESERVATION_NOT_FOUND`) / 본인 소유 아님 `403`(`REVIEW_NOT_OWNER`) / 체크인(QR 사용) 완료 좌석이 1개도 없음 `403`(`REVIEW_NOT_ELIGIBLE`) |
| 보안 | Role `MEMBER`, 본인 소유만 |
| 상태 | DONE |

## 15. 내 예약 조회 (Reservation-Service)

### `getMyReservations` — `GET /api/reservations/my`

| 항목 | 정의 |
| --- | --- |
| 정상 | `200` — 로그인 회원 본인의 예약 전체(HOLD/QUEUED/CONFIRMED/CANCELLED)를 최신순으로 반환. 각 항목에 `paidAmount`(결제 이력 없으면 `null`)·`refundedAmount`(누적, 결제 이력 없으면 `null`) 포함 — 화면에서 `paidAmount - refundedAmount`로 실결제 금액을 계산해 표시하고, `status=HOLD`는 "결제 대기"로 별도 표기. 대기열 화면(`QueueStatus`)이 순번 조회 404 이후 승격·취소·결제완료 여부를 판단할 때도 이 API로 상태를 재조회한다 |
| 보안 | Role `MEMBER`(JWT에서 memberId 추출, 쿼리 파라미터 방식 아님) |
| 상태 | DONE |

## 16. 정산 대시보드·PG 키 관리 (Reservation-Service, 전체 `ADMIN`)

### `getSettlementDashboard` — `GET /api/admin/settlements`

| 항목 | 정의 |
| --- | --- |
| 관련 Story | Story 18 / [결제완료-기준집계] |
| Request | Query: `startDate`, `endDate`(둘 다 선택, `yyyy-MM-dd`) |
| 정상 | `200` — `{totalAmount}`. `CONFIRMED` 예약의 결제 금액만 합산(취소 건은 집계 자체에서 제외, 순감산이 아니라 제외) |
| 보안 | Role `ADMIN` |
| 상태 | DONE |

### `getSettlementDetails` — `GET /api/admin/settlements/details`

| 항목 | 정의 |
| --- | --- |
| Request | Query: `startDate`, `endDate`, `page`, `size`(기본 20) |
| 정상 | `200` — 결제 건별 상세(+`meta.pagination`): `{reservationId, sessionId, memberId, amount, paidAt, reservationStatus, refundedAmount, refundedAt, ticketCount, checkedInCount}`. `CANCELLED`(환불) 건도 함께 노출(대시보드 합계와 달리 상세 이력은 제외하지 않음) |
| 보안 | Role `ADMIN` |
| 상태 | DONE |

### `registerPgCredential` — `PATCH /api/admin/settings/pg-key`

| 항목 | 정의 |
| --- | --- |
| 관련 Story·규칙 | Story 19 / [pg키-비공개] |
| Request | Body: `provider`, `storeId`, `channelKey`, `apiSecret`, `webhookSecret` |
| 정상 | `200` — provider 단위 update-or-create, `apiSecret`/`webhookSecret`은 AES-GCM 암호화 후 저장. 응답의 두 Secret 필드는 뒤 4자리만 남기고 마스킹 |
| 실패 | 필수값 누락 `400` |
| 보안 | Role `ADMIN` |
| 상태 | DONE |

## 17. 서비스 간 동기 계약 (내부 API, Gateway 미노출)

| operationId | 방향 | Method/Path | Response | 실패 시 |
| --- | --- | --- | --- | --- |
| `getSessionCapacity` | Reservation→Conference | `GET /api/sessions/{sessionId}/capacity` | `{sessionId, capacity, ...}` | `503`, 홀드 저장 안 함(Fail-closed) |
| `getSessionPrice` | Reservation→Conference | `GET /api/sessions/{sessionId}/price` | 1인당 가격 | `503`(`CONFERENCE_SERVICE_UNAVAILABLE`) |
| `getSessionStartAt` | Reservation→Conference | `GET /api/sessions/{sessionId}/startat` | 세션 진행 시작 시각 | 취소·QR스캔 흐름에서 예외 처리(문서화 목적상 `503` 전제, 코드상 예외 계층 재확인 필요 항목) |
| `getConferenceIdBySession` | Reservation→Conference | `GET /api/sessions/{sessionId}/conference-id` | conferenceId | `503` |
| `getConferenceSessionIds` | Conference 내부 소비 | `GET /api/conferences/{conferenceId}/session-ids` | `List<UUID>` | - |
| `getOrganizerWithdrawalEligibility` | Member→Conference | `GET /api/organizers/{organizerId}/withdrawal-eligibility` | `{hasActiveConference}`(PENDING·APPROVED 존재 여부) | `503`(`MEMBER_WITHDRAWAL_ELIGIBILITY_CHECK_FAILED`) |
| `getAttendeeCheckinStats` | Conference→Reservation | `GET /internal/sessions/attendee-checkin-stats?sessionIds=` | `{checkedInCount, ageGroupDistribution, jobDistribution}` | `503`(`RESERVATION_SERVICE_UNAVAILABLE`), 캐시 있으면 캐시 응답 |
| `getSessionStatusSummary` | Conference→Reservation | `GET /internal/sessions/{sessionId}/status-summary` | `{holdCount, queuedCount, confirmedCount, cancelledCount, checkedInCount}` | `503`(즉시 전체 실패, 부분 응답 없음) |
| `getPaymentSummary` | Conference→Reservation | `GET /internal/sessions/payment-summary?sessionIds=` | `{totalRevenue, refundedAmount, netRevenue, confirmedCount, cancelledCount}` | `503`(`RESERVATION_SERVICE_UNAVAILABLE`) |
| `getReviewsBySessionIds` | Conference→Reservation | `GET /internal/reservations/reviews?sessionIds=` | `{reviews: string[]}`(체크인 완료 예약만) | 통계 요약 흐름에서는 실패해도 `reviews=[]`로 폴백, 단독 조회 시엔 `503` |

모든 내부 호출은 Timeout 시 Fail-closed(임의 성공 처리 금지)를 원칙으로 한다. 공용 timeout은 connect 500ms/read 1000ms이며, Gemini 호출·`ConferenceServiceClient`·`AttendeeSummaryLlmClient` 등 외부/LLM 클라이언트는 개별적으로 더 긴 timeout을 사용한다(2~15초). 재시도(retry)·서킷브레이커는 구현되어 있지 않고 전부 즉시 실패 후 커스텀 예외 → `BusinessException`(503) 변환 방식이다.

## 비동기 Event 계약

전체 Sprint 범위에 해당 없음. 세션 정원 검증·정산 집계·참석자 통계 등 모든 서비스 간 데이터 동기화는 동기 REST 호출로 확정되어 이벤트 기반 비동기 동기화는 채택되지 않았다.
