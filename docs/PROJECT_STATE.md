# BrandPilot Backend Rebuild - Project State

이 문서는 새 저장소의 현재 상태를 나타내는 단일 기준 문서입니다. 작업을 시작할 때 먼저 읽고, 단계나 설계 결정이 바뀌었을 때만 갱신합니다.

## Current checkpoint

```yaml
project: BrandPilot Backend Rebuild
status: PROJECT_BOOTSTRAP_COMPLETE
current_stage: 4
current_stage_name: 공통 기반 준비
next_action: DB_PASSWORD를 적용해 애플리케이션 기본 실행을 검증한다
last_updated: 2026-07-17
```

## 학습·코딩 진행 방식

- 사용자가 코드를 직접 타이핑하는 것이 기본입니다.
- Codex는 한 번에 한 단계만 설명하고, 필요한 파일에는 완성 코드 대신 한국어 `TODO` 주석을 작성합니다.
- 사용자는 `TODO`의 설명을 이해한 뒤 직접 구현합니다.
- 구현 후 Codex는 작성된 코드를 검토하고 오류 원인, Spring/JPA 동작 원리와 면접에서 설명할 포인트를 알려줍니다.
- 사용자가 명시적으로 전체 구현을 요청한 경우에만 Codex가 완성 코드를 작성합니다.
- 문서는 `AGENTS.md`, `docs/API_SPEC.md`, `docs/ERD_DESIGN.md`, `docs/PROJECT_STATE.md`만 유지합니다.

## 단계별 상태

| 단계 | 상태 | 결과 |
| ---: | --- | --- |
| 0. 목표·범위·회고 | `COMPLETE` | 리빌드 목적, 제외 범위와 학습 목표 확정 |
| 1. REST API 설계 | `COMPLETE` | `/api/v1` 기반 API 23개와 고정 DTO 계약 확정 |
| 2. ERD·테이블 설계 | `REVIEW` | ERDCloud 핵심 테이블 작성 완료, 저장소 문서화와 일부 보조 테이블 재검토 필요 |
| 3. Spring 프로젝트·Git | `COMPLETE` | Initializr, MySQL, Git, GitHub 연결과 Java 컴파일 완료 |
| 4. 공통 기반 | `IN_PROGRESS` | 기본 실행, Flyway, 오류 응답과 테스트 환경 구성 예정 |
| 5. 사용자·인증 | `NOT_STARTED` | 회원가입, 로그인, JWT 수명주기 |
| 6. 브랜드 워크플로 | `NOT_STARTED` | 진단부터 최종 완료와 삭제 |
| 7. 로컬 파일 저장 | `NOT_STARTED` | 로고 파일 저장·조회·삭제 |
| 8. 품질·안정성 | `NOT_STARTED` | 테스트, 트랜잭션, 동시성, 쿼리 검토 |
| 9. 면접 대비·완료 | `NOT_STARTED` | 설계 설명, 트러블슈팅과 최종 README |

## 완료된 프로젝트 기반

- 프로젝트 경로: `/Users/janghyeogjun/Documents/brandpilot/brandpilot-backend-rebuild`
- GitHub: `https://github.com/joon0329/brandpilot-backend-rebuild`
- 기본 브랜치: `main`, `origin/main` 추적
- 첫 커밋: `9fd7ef4 chore: initialize Spring Boot project`
- Java 17, Spring Boot 4.1.0, Gradle 프로젝트
- 패키지 루트: `com.brandpilot.backend`
- 의존성: MVC, JPA, Validation, Security, MySQL, Flyway
- MySQL 8 로컬 서버와 `brandpilot` 데이터베이스 생성
- DB 애플리케이션 계정 `brandpilot_app` 생성 및 `brandpilot.*` 권한 부여
- `application.yml`에서 비밀번호를 `${DB_PASSWORD}`로 주입
- Hibernate `ddl-auto: validate`, `open-in-view: false`, Flyway 활성화
- `./gradlew compileJava` 성공

## 아직 검증하지 않은 기반

- IntelliJ 또는 터미널에서 `DB_PASSWORD`를 주입한 전체 애플리케이션 실행 성공 여부
- `./gradlew test` 성공 여부
- 테스트가 로컬 개발 DB에 의존하지 않도록 분리하는 방식
- ERDCloud 결과의 저장소 내 재현 가능한 문서 또는 DDL 변환

따라서 다음 작업은 테이블 구현이 아니라 먼저 깨끗한 기준점에서 애플리케이션을 실행하고 실패가 있다면 원인을 이해하는 것입니다.

## 확정된 서비스 범위

- 이메일 기반 회원가입과 로그인
- 사용자에게 비밀번호 길이 8~64자를 요구하고 서버에서도 검증
- Access JWT 15분, 회전하는 Refresh Token 14일
- 한 사용자당 하나의 활성 로그인 세션
- 사용자 기능은 회원가입·로그인만 제공하고 프로필, 비밀번호 변경·재설정, 이메일 인증과 회원 탈퇴는 제외
- 진단 제출 성공 시 Brand를 바로 생성
- 중간 단계에서 현재 진행 상태를 조회하고 이어서 진행 가능
- 단계 순서: `NAMING → CONCEPT → STORY → LOGO → FINAL`
- 각 단계에서 최대 3회 생성, 생성당 후보 3개
- 선택은 후보 문자열이 아니라 `candidateId`로 수행
- 재생성 시 이전 후보 내용은 삭제하고 생성 메타데이터는 `SUPERSEDED`로 유지
- 선택 시 선택된 후보 하나만 남기고 생성 메타데이터는 `SELECTED`로 변경
- 브랜드와 자식 데이터는 영구 삭제
- 실제 AI와 S3 대신 Fake 생성기와 로컬 파일 저장소 사용

## 현재 ERDCloud 핵심 테이블

### users

- `user_id BIGINT UNSIGNED PK AUTO_INCREMENT`
- `email VARCHAR(254) NOT NULL UNIQUE`
- `password_hash VARCHAR(255) NOT NULL`
- `name VARCHAR(50) NOT NULL`
- `created_at DATETIME(6) NOT NULL`

### brands

- User와 `N:1` Non-Identifying 관계
- `current_step`, `status`, 낙관적 락용 `version`
- 생성·수정·완료 시간

### diagnoses

- Brand와 `1:1` Non-Identifying 관계, `brand_id` UNIQUE
- 업종, 사업 설명, 대상 고객, 고객 문제, 차별점, 희망 이미지, 요약

### candidate_generations

- Brand와 `N:1` 관계
- `stage`, `generation_number`, `status`, 생성·수정 시간
- `(brand_id, stage, generation_number)` UNIQUE
- 상태: `ACTIVE`, `SUPERSEDED`, `SELECTED`

### 단계별 후보 테이블

- `naming_candidates`
- `concept_candidates`
- `story_candidates`
- `logo_candidates`
- 모두 `candidate_generations`를 참조하며 `(generation_id, display_order)` UNIQUE
- 활성 생성에는 순서 1~3의 후보가 존재

## ERD 재검토 항목

다음 항목은 구현 전에 누락 여부와 저장 방식을 확인합니다.

- 단일 기기 로그인을 위한 `login_sessions` 또는 동등한 인증 세션 테이블
- Diagnosis의 복수 `coreValues`, `keywords`
- Concept 후보의 복수 `brandValues`
- Story 후보의 복수 `emotionalTones`
- FK 삭제 정책과 로컬 로고 파일 삭제 책임
- 주요 조회 인덱스와 후보 생성·선택 동시성 정책

이 항목들은 기존 설계를 부정하는 것이 아니라, ERDCloud에서 작성한 핵심 테이블을 실제 DDL로 옮기기 전 확인할 체크리스트입니다.

## 다음 작업 순서

1. MySQL 서버가 실행 중인지 확인한다.
2. 현재 터미널 세션에 `DB_PASSWORD`를 설정한다. 실제 값은 문서나 Git에 기록하지 않는다.
3. `./gradlew bootRun`을 실행해 기본 애플리케이션 시작 여부를 확인한다.
4. `./gradlew test`를 실행하고 테스트 DB 전략이 필요한지 판단한다.
5. ERD 재검토 항목을 하나씩 확정한다.
6. `src/main/resources/db/migration/V1__create_users.sql`부터 Flyway 마이그레이션을 작성한다.
7. DB에서 `flyway_schema_history`와 생성 테이블을 확인한다.
8. 작은 단위로 커밋하고 이 문서의 체크포인트를 갱신한다.

## 다음 Codex CLI 시작 문장

저장소 루트에서 Codex를 새로 실행한 뒤 다음과 같이 요청합니다.

```text
AGENTS.md와 docs/PROJECT_STATE.md를 읽어줘. 나는 코드를 직접 작성하며 백엔드 기초와 면접 대비를 같이 공부하고 있다. 파일을 수정하지 말고 현재 체크포인트를 요약한 다음, 다음 한 단계인 애플리케이션 기본 실행 검증만 안내해줘.
```

## 기준 문서

- `API_SPEC.md`: 상세 API 계약
- `ERD_DESIGN.md`: ERD와 테이블 설계
- 이 문서: 현재 체크포인트와 앞으로의 순서

새로운 Markdown 문서를 추가하기보다 위 세 문서를 목적에 맞게 갱신합니다.
