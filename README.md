# [팀명] 박람회(행사)예약 관리 플랫폼

멋사 백엔드 자바 24기 심화 프로젝트 — Agile + MSA로 4주간 진행하는 박람회(행사) 예약 관리 플랫폼입니다.

## 진입점

- Notion (요구사항·설계 등 15종 문서 원본): [링크]
- GitHub Project (Product Backlog·Sprint 보드): [링크]
- Issue 템플릿: [.github/ISSUE_TEMPLATE](.github/ISSUE_TEMPLATE)
- 문서 인덱스 (Notion 원본 링크 목록): [docs/README.md](docs/README.md)

## 팀 구성

| 역할 | 담당자 |
|---|---|
| 전체 관리자 (Super Admin) | [이름] |
| 박람회 관리자 (Event Admin) | [이름] |
| 고객 (User) | [이름] |

## 실행 방법

```bash
docker compose -f infra/compose.yaml up
```

(Sprint 1 진행 중 — 실행 명령은 서비스 구성 확정 후 채웁니다.)

## 기술 스택

- Frontend: React (SPA)
- Backend: Spring Boot, JPA + MyBatis
- Infra: Docker, GitHub Actions/Jenkins
