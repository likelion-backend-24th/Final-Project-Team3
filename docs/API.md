# API·통합 계약

> **작성·동기화 메타정보**
> 
> 
> Notion 원본 URL: https://www.notion.so/API-3cd73873401a80bda31be290a53d6db4?source=copy_link
> 
> Snapshot 기준 시점: `Sprint 2 Review 종료 시점 (2026-09-11)`
> 
> 동기화 시각: `2026-09-14 10:00 KST`
> 
> 직접 편집 금지: Git Snapshot은 직접 편집하지 않고 Notion 원본을 수정한 뒤 다시 동기화합니다.
> 
> 관련 Story: Sprint 1 — Story 1(`#1`), Story 2(`#3`), Story 4(`#28`), Story 5(`#35`), Story 9(`#20`), Story 10(`#6`) / Sprint 2 — Story 3(`#26`), Story 6(`#27`), Story 8(`#68`), Story 11(`#72`)
> 관련 업무 규칙: [이메일-중복-금지], [정원-초과-금지], [대기열-순번-제한], [관리자계정-승인필수], [승인-전-비공개], [소유자원-접근제한], [이메일인증-필수], [인증코드-유효성]
> 

## 공통 규약

| 항목 | 계약 |
| --- | --- |
| 인증 | Access Token(JWT, Bearer) — 로그인 후 `Authorization: Bearer <token>` 헤더로 전달. Role: `MEMBER`(참가자) / `ORGANIZER`(주최자) / `ADMIN`(전체관리자) |
| 성공 Envelope | `{ "success": true, "data": {...}, "message": "string", "traceId": "string" }` |
| 실패 Envelope | `{ "success": false, "error": { "code": "string", "message": "string" }, "traceId": "string" }` |
| Trace ID | Gateway가 요청마다 `X-Trace-Id` 생성 후 전 서비스에 전파, 응답 Envelope에도 포함 |
| 시간·Timezone | ISO-8601, `Asia/Seoul`(KST) |
| 멱등성 | Sprint 1 범위에서는 해당 API 없음(결제 관련 Story는 Sprint 2) |
| 401 Header | `WWW-Authenticate: Bearer` |
| 내부 API | Gateway·외부 OpenAPI 비노출, 호출 관계별 환경 변수 Bearer Token |
| 권한 스코프 검증 | Role별 소유 자원 제한([소유자원-접근제한] 규칙)은 Cross-cutting 공통 인가 필터로 구현(Story 8 Task 8-2, `#32`). `ORGANIZER`·`ADMIN` Role이 붙는 API에 공통 적용, 스코프 밖 요청은 일관되게 `403` |
| Refresh Token | **HttpOnly 쿠키(`refreshToken`)로 전달** — 응답 Body에 포함하지 않음. DB에는 해시(SHA-256)로 저장, 재발급마다 Rotation 적용. 이미 폐기된 토큰이 재사용되면 탈취로 간주해 해당 회원의 모든 Refresh Token을 무효화 |

## 외부 HTTP 계약 - Sprint 1 (완료)

### `listConferences` — `GET /api/conferences`

| 항목 | 정의 |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 1(`#1`) / 시나리오 1 |
| Request | Query: `keyword`, `page`, `size` |
| 정상 | `200` — 상태=승인인 컨퍼런스만 필터되어 반환 |
| 실패 | 없음(빈 목록도 `200`) |
| 보안 | 인증 불필요(방문자 접근 가능) |
| 상태 | IMPLEMENTED |
| 추가·변경 Sprint | Sprint 1 |

### `getConference` — `GET /api/conferences/{id}`

| 항목 | 정의 |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 1(`#1`) / 시나리오 1 |
| Request | Path: `id` |
| 정상 | `200` — 컨퍼런스 상세, 세션 목록 포함 |
| 실패 | 미승인·존재하지 않는 컨퍼런스는 목록·상세 어디에도 노출되지 않음 → `404`([승인-전-비공개] 규칙) |
| 보안 | 인증 불필요(방문자 접근 가능) |
| 상태 | IMPLEMENTED |
| 추가·변경 Sprint | Sprint 1 |

### `signup` — `POST /api/members/signup`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·시나리오 | Story 2(`#3`) / 시나리오 2 |
| Request | Body: `email`, `password`, `name` |
| 정상 | `201` — 참가자 계정 생성, 비밀번호는 BCrypt(Work Factor 12) 저장 |
| 실패 | 이메일 중복(Unique 제약) 시 409, 저장 안 함([이메일-중복-금지] 규칙) / 이메일 인증을 완료하지 않은 상태로 요청 시 400(MEMBER_EMAIL_NOT_VERIFIED)([이메일인증-필수] 규칙) |
| 보안 | 인증 불필요 |
| 상태 | IMPLEMENTED |
| 추가·변경 Sprint | Sprint 1 |

### `login` — `POST /api/auth/login`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·시나리오 | Story 2(`#3`) — 참가자 로그인 / Story 8(`#28`) — 주최자 로그인(같은 API 재사용, Role=`ORGANIZER` 케이스) / 시나리오 2, 8 |
| Request | Body: `email`, `password` |
| 정상 | `200` — JWT 발급. Access Token + Refresh Token 함께 발급. 참가자는 기본 payload, 주최자는 `organizerId`·소유 컨퍼런스 범위(scope) Claim이 추가된 payload |
| 실패 | 인증 실패 시 `401`(`WWW-Authenticate: Bearer`), 저장 없음 |
| 보안 | 인증 불필요(로그인 자체). 발급된 scope의 접근 제한은 Task 8-2(`#32`) 공통 인가 필터에서 검증 |
| 상태 | IMPLEMENTED |
| 추가·변경 Sprint | Sprint 1 |

### `login` — `POST /api/auth/refresh`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·시나리오 | Story 2(#3) / Task 2-1(#9) — Refresh Token 재발급 |
| Request | Body: refreshToken |
| 정상 | `200` — 새 Access Token + Refresh Token 쌍 발급(Rotation: 기존 Refresh Token은 즉시 폐기) |
| 실패 | 존재하지 않거나 만료된 토큰 제출 시 `401`. 이미 폐기(rotate)된 토큰이 다시 제출되면 탈취로 간주해 해당 회원의 모든 Refresh Token을 즉시 무효화한 뒤 `401` 반환(재로그인 필요) |
| 보안 | 인증 불필요 — Access Token이 아닌 Refresh Token 자체가 인증 수단. Role 검증 없음(토큰 소유 여부로만 판별) |
| 상태 | IMPLEMENTED |
| 추가·변경 Sprint | Sprint 1 |

### `createHold` — `POST /api/reservations/hold`

|  항목  |  정의  |
| --- | --- |
|  Owner  |  Reservation-Service  |
|  관련 Story·시나리오  |  Story 9(#20) / 시나리오 39 |
|  Request  |  Body: sessionId, memberId, headcount  |
|  정상  | `201` — 동시 요청 상황에서도 확정 건수 ≤ 세션 정원을 만족하는 홀드 생성. HOLD는 생성 후 10분간 유효하며, 만료 시 자동 취소되어 좌석이 반환된다 |
|  실패  | 정원 초과 시 `409`— 별도 API 호출 없이 대기열(`QUEUED`) 자동 등록, queuePosition 포함하여 응답 (설계 초안의 joinQueue는 createHold로 통합됨), `headcount`가 세션 정원을 초과하는 경우 `400`(`SESSION_CAPACITY_EXCEEDED`) |
|  보안  | Role: MEMBER, 로그인 필요 |
|  상태  | IMPLEMENTED |
|  추가·변경 Sprint  |  Sprint 1  |

### `getQueuePosition` — `GET /api/reservations/{id}/queue-position`

| 항목 | 정의 |
| --- | --- |
| Owner | Reservation-Service |
| 관련 Story·시나리오 | Stroy9(`#20`), Story 10(`#6`) / 시나리오9, 시나리오 10 |
| Request | Path: `id`(reservationId) |
| 정상 | `200` — 대기 순번 반환. 순번 도달 전 결제 API 호출은 거부([대기열-순번-제한] 규칙) |
| 실패 | 대상 없음 `404` |
| 보안 | Role: `MEMBER`, 본인 소유 자원만 조회 가능 |
| 상태 | IMPLEMENTED |
| 추가·변경 Sprint | Sprint 1 |

### `createConference` — `POST /api/conferences`

| 항목 | 정의 |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 9(`#35`) / 시나리오 9 |
| Request | Body: 컨퍼런스 기본 정보(명칭, 일정, 소개, `imageUrl`(선택) 등) — 소유자는 요청 주최자로 자동 설정. `tags`(카테고리) 최소 1개 필수 |
| 정상 | `201` — 상태=신청(`PENDING`)으로 저장. 신청 직후에는 `listConferences`/`getConference`에 노출되지 않음([승인-전-비공개] 규칙) |
| 실패 | 필수값 누락 시 `400`(카테고리 미선택 포함) |
| 보안 | Role: `ORGANIZER`, 로그인 필요, 권한 스코프 검증(Task 8-2 `#32`) 적용 |
| 상태 | IMPLEMENTED |
| 추가·변경 Sprint | Sprint 1 |

## **외부 HTTP 계약 — Sprint 2**

### **`signupOrganizer` — `POST /api/members/organizers/signup`**

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·시나리오 | Story 3(#26) / 시나리오 3(13) |
| Request | Body: email, password, name, organizationName, businessNo |
| 정상 | 201 — 사업자등록번호 검증을 통과하면 관리자 승인 없이 즉시 Role=ORGANIZER 계정 생성, 자신의 컨퍼런스 범위로 권한 제한. Response: { memberId, email, name, organizationName, businessNo, role } |
| 실패 | 사업자등록번호 형식 오류 시 400(MEMBER_INVALID_BUSINESS_NO) / 이메일 또는 사업자등록번호 중복 시 409(MEMBER_DUPLICATE_EMAIL / MEMBER_DUPLICATE_BUSINESS_NO) ([사업자번호-인증필수], [주최자가입-중복금지] 규칙) |
| 보안 | 인증 불필요(공개 가입 엔드포인트) |
| 舊 계약 폐기 | POST /api/members/organizers(전체관리자가 생성해서 저장) 방식은 사용 안 함 |
| 상태 | IMPLEMENTED |
| 추가·변경 Sprint | Sprint 2 |

### **`createSession` — `POST /api/conferences/{conferenceId}/sessions`**

| **항목** | **정의** |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 5(`#35`) / 시나리오 5 |
| Request | Path: `conferenceId` / Body: `title`, `capacity`, `startAt`(신청 시작일), `endAt`(신청 종료일) |
| 정상 | `201` — 세션 생성(기본 상태 `PENDING`), 정원·기간이 참가자 신청 화면(`getSessionCapacity` 등)에 즉시 반영 |
| 실패 | 정원 ≤ 0이거나 `startAt` ≥ `endAt`이면 `400`([세션정원-유효성] 규칙) / 컨퍼런스가 `APPROVED` 상태가 아니면 `409` / 요청자가 해당 컨퍼런스 소유 주최자가 아니면 `403`([소유자원-접근제한] 규칙) |
| 보안 | Role: `ORGANIZER`, 로그인 필요, 권한 스코프 검증(Task 8-2 `#32`) 적용 |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### **`updateSession` — `PATCH /api/sessions/{sessionId}`**

| **항목** | **정의** |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 5(`#35`) / 시나리오 5 |
| Request | Path: `sessionId` / Body: `capacity`, `startAt`, `endAt` 중 변경할 값 |
| 정상 | `200` — 정원·신청 기간 수정, 참가자 신청 화면에 즉시 반영 |
| 실패 | 정원 ≤ 0이거나 `startAt` ≥ `endAt`이면 `400`([세션정원-유효성] 규칙) / 요청자가 해당 세션이 속한 컨퍼런스의 소유 주최자가 아니면 `403`([소유자원-접근제한] 규칙) / 존재하지 않는 세션이면 `404` |
| 보안 | Role: `ORGANIZER`, 로그인 필요, 권한 스코프 검증(Task 8-2 `#32`) 적용 |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### `listPendingConferences` — `GET /api/admin/conferences?status=PENDING`

| 항목 | 정의 |
| --- | --- |
| 항목 | 정의 |
| Owner | Conference-Service (오케스트레이션 없이 Conference-Service가 직접 처리) |
| 관련 Story·시나리오 | Story 6(#27) / 시나리오 6(14) |
| Request | Query: status=PENDING(고정), page, size |
| 정상 | 200 — PENDING 상태 컨퍼런스 목록 반환 |
| 실패 | 없음(빈 목록도 200) |
| 보안 | Role: ADMIN |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### `approveConference` — `PATCH /api/admin/conferences/{id}/approve`

| 항목 | 정의 |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 6(#27) / 시나리오 6(14) |
| Request | Path: id |
| 정상 | 200 — 상태를 APPROVED로 변경, 참가자에게 공개 |
| 실패 | 이미 결정된 컨퍼런스 재요청 시 409 |
| 보안 | Role: ADMIN |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### `rejectConference` — `PATCH /api/admin/conferences/{id}/reject`

| 항목 | 정의 |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 6(#27) / 시나리오 6(14) |
| Request | Path: id / Body: reason(반려 사유) |
| 정상 | 200 — 상태를 REJECTED로 변경, 사유 저장 |
| 실패 | 이미 결정된 컨퍼런스 재요청 시 409 |
| 보안 | Role: ADMIN |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### **`listPendingSessions` — `GET /api/admin/sessions?status=PENDING`**

| **항목** | **정의** |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 8(`#68`) / 시나리오 8 |
| Request | Query: `status=PENDING`(고정), `page`, `size` |
| 정상 | `200` — `PENDING` 상태 세션 목록 반환 |
| 실패 | 없음(빈 목록도 `200`) |
| 보안 | Role: `ADMIN` |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### **`approveSession` — `PATCH /api/admin/sessions/{id}/approve`**

| **항목** | **정의** |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 8(`#68`) / 시나리오 8 |
| Request | Path: `id` |
| 정상 | `200` — 상태를 `APPROVED`로 변경, 참가자에게 공개 |
| 실패 | 이미 결정된 세션 재요청 시 `409` |
| 보안 | Role: `ADMIN` |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### **`rejectSession` — `PATCH /api/admin/sessions/{id}/reject`**

| **항목** | **정의** |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 8(`#68`) / 시나리오 8 |
| Request | Path: `id` / Body: `reason`(반려 사유) |
| 정상 | `200` — 상태를 `REJECTED`로 변경, 사유 저장 |
| 실패 | 이미 결정된 세션 재요청 시 `409` |
| 보안 | Role: `ADMIN` |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### **`updateConferenceDescription` — `PATCH /api/conferences/{id}/description`**

| **항목** | **정의** |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 5(`#35`) / 시나리오 5 |
| Request | Path: id / Body: description |
| 정상 | `200` — 소개글 수정, 승인 여부와 무관하게 항상 가능 |
| 실패 | 요청자가 소유 주최자가 아니면 403([소유자원-접근제한] 규칙) / 존재하지 않는 컨퍼런스면 404 |
| 보안 | Role: ORGANIZER, 로그인 필요, 권한 스코프 검증(Task 8-2 #32) 적용 |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### **`updateConferenceLocation` — `PATCH /api/conferences/{id}/location`**

| **항목** | **정의** |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 5(`#35`) / 시나리오 5 |
| Request | Path: id / Body: location(주소), transportation(교통편), parkingInfo(주차 안내), amenities(편의시설) |
| 정상 | `200` — 네 필드 모두 수정. 단 location 값이 기존 저장값과 같으면(=주소 변경 아님) 나머지 세 필드만 바뀌어도 정상 처리 |
| 실패 | 컨퍼런스 status가 승인인 상태에서 location 값이 기존과 다르면 요청 전체 409([장소-수정불가] 규칙) / 요청자가 소유 주최자가 아니면 403 / 존재하지 않으면 404 |
| 보안 | Role: ORGANIZER, 로그인 필요, 권한 스코프 검증(Task 8-2 #32) 적용 |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### **`createNotice` — `POST /api/conferences/{id}/notices`**

| **항목** | **정의** |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 5(#35) / 시나리오 5 |
| Request | Path: id / Body: title, content |
| 정상 | `201` — 공지 등록 |
| 실패 | 요청자가 소유 주최자가 아니면 403 / 필수값 누락 400 |
| 보안 |  Role: ORGANIZER, 권한 스코프 검증 적용 |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### **`listNotices` — `GET /api/conferences/{id}/notices`**

| **항목** | **정의** |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 5(#35) / 시나리오 5 |
| Request | Path: id |
| 정상 | `200` — 공지 목록(최신순) 반환 |
| 실패 |  없음(빈 목록도 200) |
| 보안 | 인증 불필요(참가자·방문자도 조회) |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### **`updateNotice` — `PATCH /api/conferences/{id}/notices`**

| **항목** | **정의** |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 5(#35) / 시나리오 5 |
| Request | Path: id, noticeId / Body: title, content |
| 정상 | 200 — 공지 수정, updated_at 갱신 |
| 실패 | 요청자가 소유 주최자가 아니면 403 / 존재하지 않으면 404 |
| 보안 | Role: ORGANIZER, 권한 스코프 검증 적용 |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### **`deleteNotice` — `DELETE /api/conferences/{id}/notices/{noticeId}`**

| **항목** | **정의** |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 5(#35) / 시나리오 5 |
| Request | Path: id, noticeId |
| 정상 |  204 — 공지 삭제 |
| 실패 | 요청자가 소유 주최자가 아니면 403 / 존재하지 않으면 404 |
| 보안 | Role: ORGANIZER, 권한 스코프 검증 적용 |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### **`createFaq` —** `POST /api/conferences/{id}/faqs`

| **항목** | **정의** |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 5(#35) / 시나리오 5 |
| Request | Path: id / Body: question, answer |
| 정상 | 201 — FAQ 등록 |
| 실패 | 요청자가 소유 주최자가 아니면 403 / 필수값 누락 400 |
| 보안 | Role: ORGANIZER, 권한 스코프 검증 적용 |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### **`listFaqs` —** `GET /api/conferences/{id}/faqs`

| **항목** | **정의** |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 5(#35) / 시나리오 5 |
| Request | Path: id |
| 정상 | 200 — FAQ 목록(최신순) 반환 |
| 실패 |  없음(빈 목록도 200) |
| 보안 | 인증 불필요(참가자·방문자도 조회) |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### **`updateFaq` —** `PATCH /api/conferences/{id}/faqs/{faqId}`

| **항목** | **정의** |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 5(#35) / 시나리오 5 |
| Request | Path: id, faqId / Body: question, answer |
| 정상 | 200 — FAQ 수정, updated_at 갱신 |
| 실패 | 요청자가 소유 주최자가 아니면 403 / 존재하지 않으면 404 |
| 보안 | Role: ORGANIZER, 권한 스코프 검증 적용 |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### **`deleteFaq` —** `DELETE /api/conferences/{id}/faqs/{faqId}`

| **항목** | **정의** |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 5(#35) / 시나리오 5 |
| Request | Path: id, faqId |
| 정상 | 204 — FAQ 삭제 |
| 실패 | 요청자가 소유 주최자가 아니면 403 / 존재하지 않으면 404 |
| 보안 | Role: ORGANIZER, 권한 스코프 검증 적용 |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### **`processPayment` — `POST /api/reservations/{id}/payment`**

| **항목** | **정의** |
| --- | --- |
| Owner | reservation-Service |
| 관련 Story·시나리오 | Story 11(`#73`) / 시나리오 11 |
| Request | Path: id (`reservationId`) / Body: `paymentMethod`(결제 수단), `amount`(결제 금액) |
| 정상 | `200` — 결제 완료, 좌석 확정, QR 티켓 발급, 
대기열에 있던 예약이 결제 완료되면 대기열에서 제거되고, 
뒤에 있는 대기자들의 순번이 자동으로 당겨짐, 
조건부 UPDATE로 처리되어 동시 요청 시에도 정확히 한 건만 확정됨 |
| 실패 | 순번 미도달 시 `403`(`QUEUE_POSITION_NOT_REACHED`),
이미 결제 완료된 건 재요청 시 `409`(`ALREADY_CONFIRMED`),
정원이 이미 소진되어 재확인 실패 시 `409`(`SESSION_CAPACITY_EXCEEDED`,
`HOLD` 만료 후 대기열 승계로 좌석이 이미 다른 사람에게 배정된 경우) ,
`HOLD`상태로 10분 경과 시 자동 취소되며, 취소된 예약에 대한 결제 시도는 `404` |
| 보안 | Role: `MEMBER` |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### **`getQrTickets` — `GET /api/reservations/{id}/qr-tickets`**

| **항목** | **정의** |
| --- | --- |
| Owner | reservation-Service |
| 관련 Story·시나리오 | Story 11(`#73`) / 시나리오 11 |
| Request | Path: id (`reservationId`) |
| 정상 | `200` — 발급된 QR 티켓 목록 반환 |
| 실패 | 결제 미완료 상태에서 qr 요청 시  `404` |
| 보안 | Role: `MEMBER` |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### **`getMyReservations`— `GET /api/reservations/my`**

|  항목  |  정의  |
| --- | --- |
|  Owner  |  Reservation-Service  |
|  관련 Story·시나리오  |  마이페이지 지원 (Story 11 관련, 신규 발견 요구사항)  |
|  Request  |  Query: `memberId` |
|  정상  |  `200`— 해당 회원이 신청한 예약 전체(HOLD/QUEUED/CONFIRMED/CANCELLED)를 최신순(createdAt 내림차순)으로 반환  |
|  실패  | memberId 누락 또는 UUID 형식 오류 시 400 |
|  보안  |  Role: MEMBER, 현재는 Gateway 인증 미연동으로 memberId를 쿼리 파라미터로 받는 임시 방식. 추후 인증 연동 완료 시 헤더 기반으로 변경 예정  |
|  상태  |  IMPLEMENTED  |
|  추가·변경 Sprint  |  Sprint 2  |

### **`getCapacityStatus`— `GET /api/reservations/sessions/{sessionId}/capacity-status`**

|  항목  |  정의  |
| --- | --- |
|  Owner  |  Reservation-Service  |
|  관련 Story·시나리오  |  신규 발견 요구사항 (확정 예약 수·잔여좌석 집계 API 없음)  |
|  Request  |  Path: `sessionId` |
|  정상  | `200` — 세션 정원(capacity), 확정 인원(confirmedCount), 잔여좌석(remaining) 반환  |
|  실패  |  Conference-Service 조회 실패 시 `503`(Fail-closed), 세션 정보 없을 시 confirmedCount는 0으로 반환  |
|  보안  |  인증 불필요 (공개 정보)  |
|  상태  |  IMPLEMENTED  |
|  추가·변경 Sprint  |  Sprint 2  |

### **`sendVerificationCode`— `POST /api/auth/email/send-code`**

| **항목** | **정의** |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·시나리오 | Story 2(`#3`) / 이메일 인증 시나리오 |
| Request | Body: email |
| 정상 | `200` — 6자리 인증코드 발급 후 이메일로 발송(유효기간 10분). 재발송 시 기존 코드는 무효화됨 |
| 실패 | 이미 가입된 이메일이면 409(MEMBER_DUPLICATE_EMAIL, [이메일-중복-금지] 규칙) |
| 보안 | 인증 불필요(공개 엔드포인트) |
| 상태 | IMPLEMENTED |
| 추가·변경 Sprint | Sprint 2 |

### **`verifyEmailCode`— `POST /api/auth/email/verify`**

| **항목** | **정의** |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·시나리오 | Story 2(`#3`) / 이메일 인증 시나리오 |
| Request | Body: email, code |
| 정상 | `200` — 코드 일치 시 해당 이메일을 인증완료 상태로 마킹(회원가입 전 단계) |
| 실패 | 발급 이력 없음·코드 불일치 시 400(AUTH_EMAIL_CODE_INVALID), 만료 시      400(AUTH_EMAIL_CODE_EXPIRED), 5회 실패 시 429(AUTH_EMAIL_CODE_ATTEMPTS_EXCEEDED, 이후 재발송 필요)([인증코드-유효성] 규칙) |
| 보안 | 인증 불필요(공개 엔드포인트) |
| 상태 | IMPLEMENTED |
| 추가·변경 Sprint | Sprint 2 |

## **외부 HTTP 계약 — Sprint 3**

### **`registerPgCredential` — `PATCH /api/admin/settings/pg-key`**

| **항목** | **정의** |
| --- | --- |
| Owner | Reservation-Service |
| 관련 Story·시나리오 | Story 19 / 시나리오 19 |
| Request | Body: `provider`, `apiKey`, `secretKey` |
| 정상 | `200` — 키 저장(update-or-create, provider 단위 1건 유지), 응답 Body의 `secretKey`는 마스킹 처리 |
| 실패 | 필수값 누락 시 `400` |
| 보안 | Role: `ADMIN` |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 3 |

### `cancelReservation` — `POST /api/reservations/{id}/cancel`

| 항목 | 정의 |
| --- | --- |
| Owner | Reservation-Service |
| 관련 Story | Story 12 |
| Request | Path: id |
| 정상 | 200 — 환불율 계산(CONFIRMED) 또는 즉시 취소(HOLD/QUEUED), 좌석 반환, 대기열 순번 당김 |
| 실패 | 409(ALREADY_CANCELLED) |
| 상태 | PLANNED |
| 추가·변경 Sprint | Sprint 3 |

### `scanQrTicket` — `POST /api/reservations/qr-tickets/{code}/scan`

| 항목 | 정의 |
| --- | --- |
| Owner | Reservation-Service |
| 관련 Story·시나리오 | Story 13 / 시나리오 13 |
| Request | Path: code |
| 정상 | 200 — 유효한 티켓, used=true로 변경, 입장 처리 완료 |
| 실패 | 존재하지 않는 code 404(QR_TICKET_NOT_FOUND), 이미 사용된 티켓 409(QR_TICKET_ALREADY_USED) |
| 보안 | 인증 방식 미정(입장 담당자용 기기 인증 필요할 수 있음) |
| 상태 | PLANNED |
| 추가·변경 Sprint | Sprint 3 |

### `getAttendeeCheckinStats` — `GET /internal/sessions/attendee-checkin-stats?sessionIds=...`

| 항목 | 정의 |
| --- | --- |
| Owner | Reservation-Service |
| 관련 Story | Story 15 |
| Request | Query: sessionIds (배열) |
| 정상 | 200 — 체크인 완료(QR 사용) 좌석의 연령대·직무 분포 집계 |
| 상태 | PLANNED |
| 추가·변경 Sprint | Sprint 3 |

### `getPaymentSummary` — `GET /internal/sessions/payment-summary?sessionIds=...`

| 항목 | 정의 |
| --- | --- |
| Owner | Reservation-Service |
| 관련 Story | Story 17 |
| Request | Query: sessionIds (배열) |
| 정상 | 200 — 결제완료(CONFIRMED) 매출 집계, 환불(CANCELLED) 금액 제외 |
| 상태 | PLANNED |
| 추가·변경 Sprint | Sprint 3 |

### `getAttendeeSummary` — `GET /api/conferences/{conferenceId}/attendee-summary`

| 항목 | 정의 |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 15 / 시나리오 15 |
| Request | Path: conferenceId |
| 정상 | 200 — 체크인 완료(QR 사용 완료)된 좌석만 집계한 연령대·직무 분포 + AI 요약 문장 반환. 이전 조회 이후 체크인 수 변동이 없으면 저장된 요약을 그대로 재사용(LLM 재호출 안 함). 체크인 0명이면 분포는 빈 객체, summaryText는 고정 안내 문구("아직 체크인한 참석자가 없습니다")를 반환하고 LLM은 호출하지 않음 |
| 실패 | 요청자가 해당 컨퍼런스의 소유 주최자가 아니면 403([소유자원-접근제한] 규칙) / 존재하지 않는 컨퍼런스면 404 / LLM 호출 실패·Timeout 시에도 요청 자체는 200으로 반환하되 summaryText만 고정 실패 문구("요약 생성에 실패했습니다. 집계 수치를 참고해주세요.")로 대체  |
| 보안 | Role: ORGANIZER, 로그인 필요, 본인 소유 컨퍼런스만(권한 스코프 검증 적용) |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 3 |

## 서비스 간 동기 계약

### `getSessionCapacity` — `Reservation-Service → Conference-Service, GET /internal/conferences/{id}/capacity` (내부 API)

- 관련 Story·업무 규칙: Story 3(`#20`) / [정원-초과-금지]
- Request·Response: Request `id`(세션 또는 컨퍼런스 ID, 세션 단위 정원 반환) / Response `{ capacity, confirmedCount, availableSlots }`
- 내부 인증: 환경 변수 Bearer Token
- Timeout·재시도: Timeout 3s, 재시도 없음(즉시 Fail-closed) — 값 자체는 이번 Task 범위에서 최종 확정
- 실패 시 사용자 결과와 저장 여부: `503` 반환, `createHold` 저장하지 않음(성공으로 간주하지 않음)
- 상태·추가 Sprint: TODO / Sprint 1

### `getAttendeeCheckinStats` — `Conference-Service → Reservation-Service, GET /internal/sessions/attendee-checkin-stats?sessionIds={ids}` (내부 API)

- 관련 Story·업무 규칙: Story 15 / [AI요약-실참석기준]
- Request·Response: Request sessionIds(콤마 구분, 해당 컨퍼런스 소속 전체 세션 ID) / Response { checkedInCount, ageGroupDistribution: {...}, jobDistribution: {...} } — 체크인 완료(QR 사용 완료)된 좌석만 집계
- 내부 인증: 환경 변수 Bearer Token
- Timeout·재시도: Timeout 3s, 재시도 없음
- 실패 시 사용자 결과와 저장 여부: 실패 시 기존 conference_attendee_summary 캐시가 있으면 캐시 그대로 응답, 없으면 요약 생성 실패로 처리(LLM 호출 자체를 하지 않음)
- 상태·추가 Sprint: TODO / Sprint 3

## 비동기 Event 계약

Sprint 1 범위에는 해당 없음. 결정 기록상 세션 정원 검증은 동기 계약으로 확정되어 이벤트 기반 비동기 동기화는 채택되지 않았습니다.