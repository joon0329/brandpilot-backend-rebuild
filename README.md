# BrandPilot Backend Rebuild

BrandPilot은 브랜드 구축 경험이 부족한 사용자가 진단, 네이밍, 콘셉트, 스토리와 로고 단계를 차례로 진행해 하나의 브랜드 결과를 완성하는 서비스입니다.

이 저장소는 기존 BrandPilot 백엔드를 복사하거나 단순 리팩터링하는 프로젝트가 아닙니다. 백엔드 개발을 쉰 뒤 잊어버린 기본기를 다시 익히고, 기존 프로젝트에서 아쉬웠던 구조와 품질 문제를 보완하기 위해 요구사항부터 새로 설계하고 직접 구현하는 학습 프로젝트입니다.

## 문서 지도

| 문서 | 역할 | 갱신 시점 |
| --- | --- | --- |
| [AGENTS.md](AGENTS.md) | Codex가 자동으로 읽는 작업 규칙 | 진행 방식이 바뀔 때만 |
| [PROJECT_PLAN.md](docs/PROJECT_PLAN.md) | 목적, 학습 방식, 보완 목표, 로드맵과 완료 기준 | 프로젝트 방향이 바뀔 때만 |
| [PROJECT_STATE.md](docs/PROJECT_STATE.md) | 현재 체크포인트, 검증 결과, 다음 한 단계와 미결정 사항 | 검증된 체크포인트 또는 커밋 시점 |
| [API_SPEC.md](docs/API_SPEC.md) | REST API의 외부 계약 | API 계약이 바뀔 때 |
| [ERD_DESIGN.md](docs/ERD_DESIGN.md) | 테이블, 관계, 제약조건과 데이터 결정 | 데이터 설계가 바뀔 때 |

각 사실은 한 문서에서만 관리합니다. 진행 상태를 API나 ERD 문서에 반복해서 적지 않고, API 필드나 테이블 컬럼을 `PROJECT_STATE.md`에 복사하지 않습니다.

## 현재 위치

현재 구현 상태와 다음 작업은 [PROJECT_STATE.md](docs/PROJECT_STATE.md)만 확인합니다.

## 기술 스택

- Java 17
- Spring Boot 4.1.0
- Gradle
- Spring MVC, Spring Data JPA, Validation, Security
- MySQL 8
- Flyway

## 로컬 실행

MySQL 서버와 `brandpilot` 데이터베이스가 필요합니다. DB 비밀번호는 Git에 기록하지 않고 환경변수로 전달합니다.

```bash
export DB_PASSWORD='로컬에서 설정한 비밀번호'
./gradlew bootRun
```

테스트:

```bash
./gradlew test --no-daemon
```

실제 비밀번호, JWT, Refresh Token과 개인정보는 문서·코드·로그·커밋에 남기지 않습니다.
