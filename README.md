# [삼다수] IT 컨퍼런스 예약 관리 플랫폼

멋사 백엔드 자바 24기 심화 프로젝트 — Agile + MSA로 4주간 진행하는 IT/테크 컨퍼런스 예약 관리 플랫폼입니다.

## 진입점

- Notion (요구사항·설계 등 15종 문서 원본): [링크]
- GitHub Project (Product Backlog·Sprint 보드): [링크]
- Issue 템플릿: [.github/ISSUE_TEMPLATE](.github/ISSUE_TEMPLATE)
- 문서 인덱스 (Notion 원본 링크 목록): [docs/README.md](docs/README.md)

## 팀 구성

| 담당자 | 담당 서비스 | 주요 기능
|---|---|
| 지선 | Gateway, Member-Service | 인증/인가, 회원가입·로그인, API 라우팅
| 기혁 | Conference-Service | 컨퍼런스·세션 등록/조회, 승인 연동
| 동욱 | Reservation-Service | 세션 신청·결제·대기열·QR 발급
| 시환 | 전체 관리자 기능 | 계정 발급, 컨퍼런스 승인, 정산 대시보드

## 실행 방법

```bash
docker compose -f infra/compose.yaml up
```

(Sprint 1 진행 중 — 실행 명령은 서비스 구성 확정 후 채웁니다.)

## 기술 스택

- Frontend: React (SPA)
- Backend: Spring Boot, JPA + MyBatis
- Infra: Docker, GitHub Actions/Jenkins
