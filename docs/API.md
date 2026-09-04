# API·통합 계약

> **작성·동기화 메타정보**
> 
> 
> Notion 원본 URL: `https://www.notion.so/API-3cd73873401a80bda31be290a53d6db4?source=copy_link`
> 
> Snapshot 기준 시점: `Sprint 1 Review 종료 시점 (2026-09-04)`
> 
> 동기화 시각: `2026-09-04 18:30 KST`
> 
> 직접 편집 금지: Git Snapshot은 직접 편집하지 않고 Notion 원본을 수정한 뒤 다시 동기화합니다.
> 
> 관련 Story: Sprint 1 — Story 1(`#1`), Story 2(`#3`), Story 3(`#20`), Story 4(`#6`), Story 8(`#28`), Story 9(`#35`), Story 13(`#26`), Story 14(`#27`)
> 관련 업무 규칙: [이메일-중복-금지], [정원-초과-금지], [대기열-순번-제한], [관리자계정-승인필수], [승인-전-비공개], [소유자원-접근제한]
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
| Refresh Token | DB에 해시(SHA-256)로 저장, 재발급마다 Rotation 적용. 이미 폐기된 토큰이 재사용되면 탈취로 간주해 해당 회원의 모든 Refresh Token을 무효화 |

## 외부 HTTP 계약

### `listConferences` — `GET /api/conferences`

| 항목 | 정의 |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 1(`#1`) / 시나리오 1 |
| Request | Query: `keyword`, `page`, `size` |
| 정상 | `200` — 상태=승인인 컨퍼런스만 필터되어 반환 |
| 실패 | 없음(빈 목록도 `200`) |
| 보안 | 인증 불필요(방문자 접근 가능) |
| 상태 | TODO |
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
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 1 |

### `signup` — `POST /api/members/signup`

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·시나리오 | Story 2(`#3`) / 시나리오 2 |
| Request | Body: `email`, `password`, `name` |
| 정상 | `201` — 참가자 계정 생성, 비밀번호는 BCrypt(Work Factor 12) 저장 |
| 실패 | 이메일 중복(Unique 제약) 시 `409`, 저장 안 함([이메일-중복-금지] 규칙) |
| 보안 | 인증 불필요 |
| 상태 | TODO |
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
| 상태 | TODO |
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
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 1 |

### `createHold` — `POST /api/reservations/hold`

|  항목  |  정의  |
| --- | --- |
|  Owner  |  Reservation-Service  |
|  관련 Story·시나리오  |  Story 3(#20) / 시나리오 3  |
|  Request  |  Body: sessionId, memberId, headcount  |
|  정상  |  201 — 동시 요청 상황에서도 확정 건수 ≤ 세션 정원을 만족하는 홀드(HOLD) 생성. 동시성 제어는 조건부 UPDATE(원자적 UPSERT + Conditional Update) 방식 채택  |
|  실패  |  Conference-Service가 정상 응답했지만 잔여 좌석이 0인 정상 매진인 경우 409 — Response body에 reservationId를 QUEUED 상태로 생성해 포함, 클라이언트는 이 id로 joinQueue 호출 / getSessionCapacity 동기 계약 Timeout·오류 시 503(Fail-closed, 홀드 생성 안 함, [정원-초과-금지]) — Task 3-2 완료 후 반영 예정, 현재는 고정 정원값 사용  |
|  보안  |  Role: MEMBER, 로그인 필요 (Gateway 인증 연동 전, 현재는 미검증 상태)  |
|  상태  |  IMPLEMENTED (정원 검증·홀드·대기열 로직) / PLANNED (Conference-Service 실 연동, Timeout 처리)  |
|  추가·변경 Sprint  |  Sprint 1  |

### `joinQueue` — `POST /api/reservations/{id}/queue`

| 항목 | 정의 |
| --- | --- |
| Owner | Reservation-Service |
| 관련 Story·시나리오 | Story 4(`#6`) / 시나리오 4 |
| Request | Path: `id`(reservationId) — `createHold`가 `409`(정원 소진)로 응답할 때 함께 반환된 `reservationId` 사용 |
| 정상 | `201` — 정원 초과 시점에 신청한 참가자를 대기열에 순번대로 등록(Redis Sorted Set 등) |
| 실패 | 이미 홀드/등록된 신청 재등록 시 `409` |
| 보안 | Role: `MEMBER`, 로그인 필요 |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 1 |

### `getQueuePosition` — `GET /api/reservations/{id}/queue-position`

| 항목 | 정의 |
| --- | --- |
| Owner | Reservation-Service |
| 관련 Story·시나리오 | Story 4(`#6`) / 시나리오 4 |
| Request | Path: `id`(reservationId) |
| 정상 | `200` — 대기 순번 반환. 순번 도달 전 결제 API 호출은 거부([대기열-순번-제한] 규칙) |
| 실패 | 대상 없음 `404` |
| 보안 | Role: `MEMBER`, 본인 소유 자원만 조회 가능 |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 1 |

### `createConference` — `POST /api/conferences`

| 항목 | 정의 |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 9(`#35`) / 시나리오 9 |
| Request | Body: 컨퍼런스 기본 정보(명칭, 일정, 소개 등) — 소유자는 요청 주최자로 자동 설정 |
| 정상 | `201` — 상태=신청(`PENDING`)으로 저장. 신청 직후에는 `listConferences`/`getConference`에 노출되지 않음([승인-전-비공개] 규칙) |
| 실패 | 필수값 누락 시 `400` |
| 보안 | Role: `ORGANIZER`, 로그인 필요, 권한 스코프 검증(Task 8-2 `#32`) 적용 |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 1 |

### **`signupOrganizer` — `POST /api/members/organizers/signup`**

| 항목 | 정의 |
| --- | --- |
| Owner | Member-Service |
| 관련 Story·시나리오 | Story 13(#26) / 시나리오 13 |
| Request | Body: email, password, name, organizationName, businessNo |
| 정상 | 201 — 사업자등록번호 검증을 통과하면 관리자 승인 없이 즉시 Role=ORGANIZER 계정 생성, 자신의 컨퍼런스 범위로 권한 제한. Response: { memberId, email, name, organizationName, businessNo, role } |
| 실패 | 사업자등록번호 형식 오류 시 400(MEMBER_INVALID_BUSINESS_NO) / 이메일 또는 사업자등록번호 중복 시 409(MEMBER_DUPLICATE_EMAIL / MEMBER_DUPLICATE_BUSINESS_NO) ([사업자번호-인증필수], [주최자가입-중복금지] 규칙) |
| 보안 | 인증 불필요(공개 가입 엔드포인트) |
| 舊 계약 폐기 | POST /api/members/organizers(전체관리자가 생성해서 저장) 방식은 사용 안 함 |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 2 |

### `listPendingConferences` — `GET /api/admin/conferences?status=PENDING`

| 항목 | 정의 |
| --- | --- |
| Owner | Conference-Service (오케스트레이션, Conference-Service에 위임) |
| 관련 Story·시나리오 | Story 14(`#27`) / 시나리오 14 |
| Request | Query: `status=PENDING`(고정), `page`, `size` |
| 정상 | `200` — `PENDING` 상태 컨퍼런스 목록 반환 |
| 실패 | 없음(빈 목록도 `200`) |
| 보안 | Role: `ADMIN` |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 1 |

### `approveConference` — `PATCH /api/admin/conferences/{id}/approve`

| 항목 | 정의 |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 14(`#27`) / 시나리오 14 |
| Request | Path: `id` |
| 정상 | `200` — 상태를 `APPROVED`로 변경, 참가자에게 공개 |
| 실패 | 이미 결정된 컨퍼런스 재요청 시 `409` |
| 보안 | Role: `ADMIN` |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 1 |

### `rejectConference` — `PATCH /api/admin/conferences/{id}/reject`

| 항목 | 정의 |
| --- | --- |
| Owner | Conference-Service |
| 관련 Story·시나리오 | Story 14(`#27`) / 시나리오 14 |
| Request | Path: `id` / Body: `reason`(반려 사유) |
| 정상 | `200` — 상태를 `REJECTED`로 변경, 사유 저장 |
| 실패 | 이미 결정된 컨퍼런스 재요청 시 `409` |
| 보안 | Role: `ADMIN` |
| 상태 | TODO |
| 추가·변경 Sprint | Sprint 1 |

## 서비스 간 동기 계약

### `getSessionCapacity` — `Reservation-Service → Conference-Service, GET /internal/conferences/{id}/capacity` (내부 API)

- 관련 Story·업무 규칙: Story 3(`#20`) / [정원-초과-금지]
- Request·Response: Request `id`(세션 또는 컨퍼런스 ID, 세션 단위 정원 반환) / Response `{ capacity, confirmedCount, availableSlots }`
- 내부 인증: 환경 변수 Bearer Token
- Timeout·재시도: Timeout 3s, 재시도 없음(즉시 Fail-closed) — 값 자체는 이번 Task 범위에서 최종 확정
- 실패 시 사용자 결과와 저장 여부: `503` 반환, `createHold` 저장하지 않음(성공으로 간주하지 않음)
- 상태·추가 Sprint: TODO / Sprint 1

## 비동기 Event 계약

Sprint 1 범위에는 해당 없음. 결정 기록상 세션 정원 검증은 동기 계약으로 확정되어 이벤트 기반 비동기 동기화는 채택되지 않았습니다.