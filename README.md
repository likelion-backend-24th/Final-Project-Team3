# [삼다수] TechConf: IT 컨퍼런스 예약 관리 플랫폼

멋사 백엔드 24기 심화 프로젝트입니다. Agile과 MSA로 4주간 진행한 IT/테크 컨퍼런스 탐색, 신청, 참여, 사후 경험 통합 플랫폼입니다.

배포 주소: https://techconf.duckdns.org

## 프로젝트 개요

| 항목 | 내용 |
| --- | --- |
| 개발 기간 | 2026.08.31 ~ 2026.09.30 (4주, Sprint 1 ~ 3 + Week 4 안정화) |
| 팀 구성 | 4인 (김지선, 차시환, 전기혁, 정동욱) |
| 주요 사용자 | 참가자(컨퍼런스를 찾아 세션을 신청하는 회원, 로그인 전 방문자 포함), 주최자(사업자 인증 후 컨퍼런스를 등록·운영하는 기관), 전체관리자(승인과 정산으로 플랫폼을 운영하는 주체) |
| 해결하려는 문제 | 참가자: 컨퍼런스·세션 정보가 주최사 홈페이지, SNS, 오픈카톡방에 흩어져 있음<br>주최자: 신청, 결제, 대기열, QR 입장을 서로 다른 도구로 처리하고, 체크인·후기를 수기로 관리함<br>공통: 행사가 끝나면 참가자·주최자·다음 컨퍼런스로 관계가 이어지지 않음 |
| 핵심 가치 | **탐색부터 재참여까지 한 곳에서**: 컨퍼런스 탐색, 세션 신청·결제, QR 입장, 후기까지 한 플랫폼에서<br>**몰려도 안전하게**: 동시 신청에도 정원을 넘지 않고, 넘치는 인원은 대기열로 자동 전환·승격<br>**AI 인사이트**: 소개글 요약과 체크인 기준 참석자 요약(Gemini), AI 장애에도 핵심 기능은 동작<br>**검증된 주최자와 안전한 거래**: 국세청 사업자 상태조회, 관리자 승인 후 공개, 각 서비스의 JWT 직접 검증, 결제 서버 재검증 |

## 진입점

- 문서 인덱스 (Notion 원본 링크와 Git Snapshot 목록): [docs/README.md](docs/README.md)
- GitHub Project (Product Backlog, Sprint 보드): https://github.com/orgs/likelion-backend-24th/projects/5
- Issue 템플릿: [.github/ISSUE_TEMPLATE](.github/ISSUE_TEMPLATE)
- API 문서: 각 서비스 기동 후 `/swagger-ui.html` (springdoc)

## 서비스 소개

참가자는 컨퍼런스를 찾아 신청하고 참여하며, 주최자는 컨퍼런스를 등록하고 운영하며, 전체관리자는 승인과 정산으로 플랫폼을 운영하는 세 역할을 하나로 묶었습니다.

### 참가자 (방문자 포함)

- 컨퍼런스 목록·상세 공개 조회 (비회원 포함), 카테고리 필터, 세션별 잔여석, 주최자 프로필(지난 컨퍼런스와 AI 요약)
- 이메일 인증 회원가입, 소셜 로그인 (Google, Kakao, 참가자 전용), 비밀번호 재설정, 소셜 계정 연동, 회원 탈퇴
- 세션 신청 (좌석별 연령대·직무 입력, 본인은 프로필로 자동 입력), 좌석 10분 홀드 후 결제 (PortOne)
- 정원 초과 시 대기 신청, 대기 순번과 결과 확인 시점 안내, 자리가 나면 결제 단계로 자동 승격
- QR 티켓 (인원수만큼 발급), 마이페이지 예약·결제·환불 내역, 예약 전체 또는 1명 단위 취소와 환불
- 체크인한 세션의 후기 작성

### 주최자

- 이메일 인증과 국세청 사업자 상태조회 (계속사업자만 가입)
- 컨퍼런스 등록 신청 (대표 배너, 소개글 이미지, 증빙 파일), 소개글 AI 요약 (Gemini)
- 세션 등록 신청과 수정, 공지·FAQ 관리
- 운영 현황 (세션별 홀드·대기·확정·체크인), 현장 QR 체크인 (카메라 스캔 또는 코드 입력)
- 참석자 통계 (체크인 기준 연령대·직무 분포)와 AI 참석자 요약, 후기 모아보기, 정산 내역 (총매출·환불액·순매출)

### 전체관리자

- 컨퍼런스·세션 승인과 반려 (증빙 파일 확인, 반려 사유)
- 회원 조회·검색과 권한 변경
- PG 연동 키 등록 (암호화 저장)
- 플랫폼 통합 정산 대시보드와 결제 건별 상세 (환불 포함)

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Backend | Java 17, Spring Boot 4.1, Spring Security, Spring Data JPA, Spring Cloud Gateway (Server WebMVC), springdoc-openapi, jjwt |
| Frontend | React 19, Vite 8, Tailwind CSS 3, React Router 7, lucide-react, qrcode.react, jsQR |
| Data | MySQL 8.0 (단일 인스턴스, 서비스별 스키마 분리) |
| 외부 연동 | PortOne V2 (결제), Google Gemini (AI 요약), 국세청 사업자 상태조회 API, Gmail SMTP (메일), OAuth (Google, Kakao) |
| Infra | Docker Compose, Nginx (HTTPS), Let's Encrypt, AWS EC2, GitHub Actions, ghcr.io |

## 아키텍처

```
브라우저 -> Nginx :443 (HTTPS, React 정적 파일)
              |
              +-> /api/** -> Gateway :8080 -> member-service       :8081  (MySQL member_service)
                                           -> conference-service   :8082  (MySQL conference_service, 업로드 볼륨)
                                           -> reservation-service  :8083  (MySQL reservation_service)
```

| 서비스 | 책임 |
| --- | --- |
| gateway | 경로 기반 서비스 라우팅, 요청별 추적 ID(`X-Trace-Id`) 부여 |
| member-service | 가입, 로그인, 토큰 재발급(Refresh Token Rotation), 이메일 인증, 소셜 로그인, 사업자 상태조회, 비밀번호 재설정, 탈퇴, 회원 관리 |
| conference-service | 컨퍼런스·세션 등록과 승인, 공지·FAQ, 이미지·증빙 파일, 소개글·참석자 AI 요약, 운영 현황, 주최자 프로필 |
| reservation-service | 세션 신청(좌석 홀드), 대기열과 자동 승격, 결제 검증(PortOne), QR 티켓·체크인, 취소·환불, 후기, 정산 |

- JWT는 Gateway를 믿지 않고 **각 서비스가 공유 비밀키로 서명을 직접 검증**합니다.
- 서비스는 각자 자기 스키마만 사용하고, 다른 서비스 데이터가 필요하면 `/internal/**` 내부 API로 요청합니다. 내부 API는 Gateway에 라우팅하지 않아 외부에 노출되지 않습니다.
- 자세한 내용은 [아키텍처](docs/아키텍처.md), [서비스 경계](docs/서비스경계.md), [시퀀스](docs/시퀀스.md)를 참고하세요.

## 프로젝트 구조

```
.
├── backend/
│   ├── gateway/               Spring Cloud Gateway
│   ├── member-service/        회원·인증
│   ├── conference-service/    컨퍼런스·세션·AI 요약
│   └── reservation-service/   신청·대기열·결제·QR·정산
├── frontend/                  React + Vite
├── infra/
│   ├── compose.yaml           전체 스택 (로컬은 소스에서 직접 빌드)
│   ├── compose.override.yaml  로컬 전용 (Gateway 8090 노출)
│   ├── compose.prod.yaml      운영 배포 (ghcr.io 이미지 사용, HTTPS)
│   ├── mysql/init/            MySQL 초기화 스크립트 (서비스별 스키마·계정 생성)
│   └── nginx/                 운영 Nginx 설정
├── docs/                      Git Snapshot 문서 (Notion 원본 기준)
└── .github/workflows/         서비스별 CI, CD
```

## 로컬 실행

사전 조건: Docker Compose v2 (서비스별 IDE 실행 시 JDK 17, Node.js)

```bash
# 1. 환경 변수 (값을 채워야 함)
cd infra
cp .env.example .env

# 2. 전체 스택 기동 (MySQL, 백엔드 4개, 프론트엔드)
docker compose up -d --build
```

- 프론트엔드: http://localhost , API(Gateway): http://localhost:8090
- `SOCIAL_MODE=mock`이면 소셜 토큰 검증을 건너뛰어 OAuth 키 없이 기동할 수 있습니다. 실제 소셜 로그인은 `real`과 OAuth 키, 프론트 빌드용 `VITE_GOOGLE_CLIENT_ID`·`VITE_KAKAO_JS_KEY`가 필요합니다.
- 주최자 가입은 국세청 상태조회를 호출하므로 공공데이터포털에서 발급받은 `NTS_SERVICE_KEY`가 필요합니다.
- 전체관리자 계정은 가입 API가 없습니다. 가입한 계정의 권한을 DB에서 `ADMIN`으로 바꾸거나, 기존 관리자가 회원 관리 화면에서 권한을 변경합니다.
- 컨퍼런스는 주최자가 등록하고 전체관리자가 승인해야 목록에 노출됩니다.

환경 변수 전체 목록과 운영 배포 방법은 [실행·배포 가이드](docs/배포가이드.md)를 참고하세요.

## 테스트

```bash
cd backend/member-service      # 또는 gateway, conference-service, reservation-service
./gradlew test
```

실행 결과와 테스트별 근거는 [테스트 체크리스트](docs/테스트체크리스트.md), 전략은 [테스트 전략](docs/테스트전략.md)에 있습니다.

## CI/CD

- **CI**: `main` 브랜치에 push하거나 PR을 올리면, 변경된 서비스(`backend/<서비스>/**`)의 테스트만 GitHub Actions가 실행합니다.
- **CD**: Actions에서 CD 워크플로를 수동 실행하면 서비스별 이미지를 빌드해 ghcr.io에 push한 뒤, EC2 서버에 SSH로 접속해 `docker compose pull`과 `up -d`를 실행합니다.
- 프론트엔드의 `VITE_*` 값은 이미지 빌드 시점에 들어가므로 GitHub Secrets로 관리합니다.

## 문서

| 문서 | 설명 |
| --- | --- |
| [요구사항](docs/요구사항.md) | 시나리오와 업무 규칙 |
| [화면 설계](docs/화면설계.md) | 화면 구성 |
| [서비스 경계](docs/서비스경계.md) | 서비스별 책임과 데이터 소유 |
| [아키텍처](docs/아키텍처.md) | 시스템 구성 |
| [ERD](docs/ERD.md) | 데이터 모델 |
| [API](docs/API.md) | HTTP와 내부 API 계약 |
| [권한 Matrix](docs/권한매트릭스.md) | 역할별 접근 권한 |
| [시퀀스](docs/시퀀스.md) | 주요 흐름 |
| [공통 완료 기준](docs/Definition_of_Done.md) | PBI별 완료 판정과 Evidence |
| [테스트 전략](docs/테스트전략.md) | 테스트 수준과 시나리오 |
| [테스트 체크리스트](docs/테스트체크리스트.md) | 테스트 실행 결과 |
| [실행·배포 가이드](docs/배포가이드.md) | 환경 변수, 로컬 실행, 운영 배포 |
| [트러블슈팅](docs/트러블슈팅.md) | 주요 문제와 해결 과정 |
| [Sprint Review](docs/sprint_review.md) | 스프린트별 결과 |
| [Sprint Retrospective](docs/sprint_retrospective.md) | 스프린트별 회고 |

문서 원본은 Notion이며 Git의 `docs/`는 Snapshot입니다. 전체 목록과 Notion 링크는 [docs/README.md](docs/README.md)를 참고하세요.

## 팀 구성

| 이름 | 역할 |
| --- | --- |
| 김지선 | 팀장 |
| 전기혁 | 팀원 | 
| 정동욱 | 팀원 | 
| 차시환 | 팀원 |
