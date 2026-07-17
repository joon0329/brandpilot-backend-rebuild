# BrandPilot Backend Rebuild - Project State

이 문서는 새 저장소의 현재 상태를 나타내는 단일 기준 문서입니다. 작업을 시작할 때 먼저 읽고, 단계나 설계 결정이 바뀌었을 때만 갱신합니다.

## Current checkpoint

```yaml
project: BrandPilot Backend Rebuild
status: SIGNUP_FOUNDATION_VERIFIED
current_stage: 5
current_stage_name: 사용자·인증
next_action: 현재 사용자 영속성·DTO·비밀번호 인코더 기반을 커밋한 뒤 SignupService를 작성한다
last_updated: 2026-07-17
```

## 학습·코딩 진행 방식

- 사용자가 코드를 직접 타이핑하는 것이 기본입니다.
- Codex는 한 번에 한 단계만 설명하고, 필요한 파일에는 완성 코드 대신 한국어 `TODO` 주석을 작성합니다.
- 사용자는 `TODO`의 설명을 이해한 뒤 직접 구현합니다.
- 구현 후 Codex는 작성된 코드를 검토하고 오류 원인, Spring/JPA 동작 원리와 면접에서 설명할 포인트를 알려줍니다.
- 사용자가 명시적으로 전체 구현을 요청한 경우에만 Codex가 완성 코드를 작성합니다.
- 문서는 `AGENTS.md`, `docs/API_SPEC.md`, `docs/ERD_DESIGN.md`, `docs/PROJECT_STATE.md`만 유지합니다.

## 기존 프로젝트 회고와 리빌드 기준

이 회고는 기존 BrandPilot을 실패한 프로젝트로 평가하기 위한 것이 아닙니다. 제한된 기간에 완성한 서비스에서 유지할 장점과 실제 백엔드 품질을 위해 보완할 점을 구분하고, 리빌드 결과를 판단하는 기준으로 사용합니다.

### 유지할 좋은 점

#### 1. 단계형 도메인

기존 서비스에는 `INTERVIEW → NAMING → CONCEPT → STORY → LOGO → FINAL`로 이어지는 명확한 비즈니스 흐름이 있었습니다. 단순 CRUD가 아니라 상태 전이와 순서 제약을 설명할 수 있는 도메인이라는 장점을 유지합니다. 리빌드에서는 진단 제출 후 Brand를 생성하고 `NAMING → CONCEPT → STORY → LOGO → FINAL`로 진행합니다.

#### 2. 사용자 소유권 검증

브랜드와 게시글 변경 시 요청 사용자와 리소스 소유자를 비교했던 방향은 유지합니다. 리빌드에서는 인증과 소유권 검증을 구분하고, 다른 사용자의 Brand 조회·변경·삭제를 서버에서 일관되게 차단합니다.

#### 3. 생성 문맥과 최종 선택의 분리

재생성 가능한 후보 문맥과 사용자가 최종 확정한 결과를 구분했던 방향은 유지할 가치가 있습니다. 리빌드에서는 `candidate_generations`와 단계별 후보 테이블을 통해 생성 회차, 활성 후보와 최종 선택의 의미를 명시합니다.

#### 4. 외부 시스템 경계 경험

AI 서버, S3, Nginx, Docker와 Kafka를 연결하며 시스템 경계를 경험했습니다. 리빌드에서는 기술 개수를 늘리기보다 Fake 생성기와 Local 파일 저장소를 인터페이스 뒤에 두고 경계, 실패와 교체 가능성을 정확히 설계합니다.

### 반드시 보완할 문제

#### 1. 요구사항과 도메인 용어가 코드보다 먼저 정리되지 않음

기존 문제:

- 기업 진단과 브랜드 컨설팅의 경계가 명확하지 않았습니다.
- 투자 라운지와 `PromotionPost`처럼 서비스 용어와 코드 용어가 달랐습니다.
- 서비스 방향이 바뀐 흔적이 프론트 경로와 백엔드 코드에 남았습니다.

리빌드 기준:

- 구현 전에 핵심 사용자, 유스케이스, 포함·제외 범위를 확정합니다.
- API, DTO, Entity와 테이블에서 같은 개념에 같은 이름을 사용합니다.
- 투자 게시판처럼 핵심 흐름과 무관한 기능은 제외합니다.

#### 2. 공개 API가 `Map<String, Object>`에 의존

기존 문제:

- 필수값, 길이와 타입 검증이 어려웠습니다.
- 프론트엔드와 백엔드 사이의 데이터 계약이 불명확했습니다.
- 런타임 캐스팅과 JSON 구조 오류 가능성이 컸습니다.

리빌드 기준:

- 유스케이스별 고정 Request/Response DTO를 사용합니다.
- Bean Validation으로 서버에서도 입력을 검증합니다.
- API 명세, 구현 DTO와 테스트의 필드가 일치해야 합니다.

#### 3. 공통 오류 응답과 HTTP 상태 코드가 불명확

기존 문제:

- `IllegalArgumentException`, `IllegalStateException` 같은 예외가 그대로 전파되었습니다.
- 입력 오류, 리소스 없음, 권한 없음과 단계 충돌이 500 응답으로 보일 수 있었습니다.

리빌드 기준:

- 도메인·애플리케이션 예외와 오류 코드를 정의합니다.
- `@RestControllerAdvice`에서 공통 오류 응답을 만듭니다.
- 400, 401, 403, 404, 409와 500을 실패 의미에 맞게 구분하고 테스트합니다.

#### 4. 실험용 Security 설정과 서비스 설정이 혼재

기존 문제:

- `/brands/**`는 `permitAll`인데 Controller는 인증 사용자가 존재한다고 가정하는 등 설정과 코드가 일치하지 않았습니다.
- JWT Authorization 헤더 전체를 로그로 출력했습니다.

리빌드 기준:

- 공개 API와 보호 API를 명확하게 구분합니다.
- Security Filter Chain과 Controller의 인증 가정을 일치시킵니다.
- 토큰, 비밀번호와 개인정보를 로그에 남기지 않습니다.
- 인증 실패 401과 인가 실패 403을 분리해 검증합니다.

#### 5. 핵심 규칙을 증명하는 테스트 부족

기존 문제:

- 상태 전이, 소유권과 실패 흐름을 자동으로 검증하지 못했습니다.
- 정상 동작 확인이 수동 호출에 의존했습니다.

리빌드 기준:

- 도메인 단위 테스트로 상태 규칙을 검증합니다.
- Service 통합 테스트로 트랜잭션과 소유권을 검증합니다.
- Controller 테스트로 입력 검증, 상태 코드와 오류 응답을 검증합니다.
- MySQL 고유 동작이 중요한 테스트에는 실제 MySQL 또는 Testcontainers 사용을 검토합니다.

#### 6. 외부 호출이 긴 DB 트랜잭션 안에 존재

기존 문제:

- AI 호출이 지연되는 동안 DB 트랜잭션과 커넥션이 유지될 수 있었습니다.
- 외부 시스템 장애가 DB 작업과 강하게 결합했습니다.

리빌드 기준:

- DB 조회·검증, 외부 후보 생성과 결과 저장의 경계를 구분합니다.
- 데이터 변경에 꼭 필요한 범위만 짧은 트랜잭션으로 묶습니다.
- 재시도 시 중복 후보가 생성되지 않도록 멱등성과 생성 회차 제약을 검토합니다.

#### 7. DB와 파일 저장소 사이의 일관성 부족

기존 문제:

- 파일 저장 후 DB 저장이 실패하면 사용되지 않는 파일이 남을 수 있었습니다.
- DB 삭제와 S3 삭제 중 한 작업만 성공할 수 있었습니다.

리빌드 기준:

- 파일 저장소를 인터페이스로 분리하고 로컬 구현을 사용합니다.
- DB 실패 시 새 파일을 제거하는 보상 처리와 삭제 실패 정책을 정의합니다.
- 파일 크기, Content-Type, 확장자, 안전한 파일명과 경로를 검증합니다.

#### 8. DB 스키마 버전 관리 부재

기존 문제:

- Hibernate `ddl-auto: update`에 의존했습니다.
- 환경별 스키마 차이와 변경 이력을 재현하기 어려웠습니다.

리빌드 기준:

- Flyway SQL로 최초 생성과 모든 스키마 변경을 관리합니다.
- Hibernate는 `ddl-auto: validate`로 Entity와 스키마 일치만 확인합니다.
- 빈 MySQL에서도 마이그레이션만으로 동일한 구조를 재현할 수 있어야 합니다.

#### 9. 도메인 동시성 고려 부족

기존 문제:

- 같은 Brand의 같은 단계를 동시에 선택할 수 있었습니다.
- 단계별 활성 컨텍스트가 하나라는 규칙이 애플리케이션 조건문에만 있었습니다.

리빌드 기준:

- 중복 생성, 중복 선택과 동시 상태 전이 시나리오를 먼저 정의합니다.
- UNIQUE 제약, 낙관적 락과 필요 시 비관적 락의 역할을 비교합니다.
- 선택한 정책을 동시성 테스트로 검증합니다.

#### 10. 계층별 책임과 의존 방향이 일관되지 않음

기존 문제:

- Controller가 Service 구현 클래스를 직접 주입하는 경우가 있었습니다.
- Entity에 Setter가 넓게 열려 있어 잘못된 상태를 만들기 쉬웠습니다.
- 사용하지 않는 클래스와 임시 주석 코드가 남았습니다.

리빌드 기준:

- Controller는 HTTP 변환과 검증 결과 전달에 집중합니다.
- Service는 유스케이스와 트랜잭션을 조율합니다.
- Entity는 생성자와 의미 있는 도메인 메서드로 상태 변경을 제한합니다.
- 인터페이스는 외부 의존성 교체나 실제 다형성이 필요한 경계에 사용하며 모든 Service에 기계적으로 만들지 않습니다.

#### 11. 로그, 관측성과 환경 설정 부족

기존 문제:

- `System.out.println` 중심의 임시 로그가 존재했습니다.
- 로그 레벨, 요청 추적과 민감 정보 정책이 없었습니다.
- 환경별 실행 설정을 문서만으로 재현하기 어려웠습니다.

리빌드 기준:

- SLF4J를 사용하고 환경별 로그 레벨을 구분합니다.
- 비밀번호, JWT와 개인정보를 기록하지 않습니다.
- 개발 환경 설정과 필수 환경변수를 재현 가능하게 관리합니다.
- 실제 운영 배포는 범위 밖이지만 운영을 방해하는 설정 습관은 남기지 않습니다.

### Kafka 실험에 대한 평가

Kafka는 실제 서비스 요구사항 때문에 필수로 선택한 것이 아니라 컨셉 생성 지연과 비동기 처리를 학습하기 위해 도입한 실험이었습니다. Producer, Topic, Partition, Consumer, Consumer concurrency와 요청 ID 기반 결과 연결을 경험한 점은 의미가 있습니다.

하지만 인메모리 Future 기반 연결은 다중 인스턴스와 장애 복구에 취약하고, 재시도·멱등성·DLQ·Consumer Group·Offset까지 설계하지 않은 상태에서 서비스 복잡도만 높였습니다. 따라서 리빌드 핵심 구조에서는 Kafka를 제외하고, 필요하면 핵심 기능 완료 후 별도 실험으로 학습합니다.

## 보완 항목 실행 현황

| 항목 | 현재 상태 | 완료 판단 근거 | 적용 단계 |
| --- | --- | --- | --- |
| 요구사항·용어 정리 | `PARTIAL` | 핵심 범위와 API는 확정, 저장소 내 도메인 용어 최종 검수 필요 | ERD 재검토 |
| 고정 DTO와 서버 검증 | `DESIGNED` | API 명세 확정, Java DTO와 Validation 구현 전 | 사용자·브랜드 기능 |
| 공통 오류 응답 | `NOT_STARTED` | 오류 계약은 API에 있으나 코드 없음 | 공통 기반 |
| Security 일관성·민감 로그 | `NOT_STARTED` | 의존성만 존재, Filter Chain과 JWT 구현 전 | 사용자·인증 |
| 자동화 테스트 | `NOT_STARTED` | 생성된 context test만 존재하며 실행 미검증 | 모든 구현 단계 |
| 외부 호출·트랜잭션 경계 | `DESIGNED` | Fake 경계 방향만 확정, 코드와 실패 테스트 없음 | 브랜드 워크플로 |
| DB·파일 일관성 | `DESIGNED` | Local 저장 방향만 확정, 보상 정책과 코드 없음 | 로컬 파일 저장 |
| Flyway 스키마 관리 | `IN_PROGRESS` | 의존성과 설정 완료, 실제 migration 없음 | 공통 기반 |
| 동시성 제어 | `PARTIAL` | Brand `version`과 일부 UNIQUE 계획, 정책·테스트 미완료 | ERD·품질 단계 |
| 계층 책임·도메인 캡슐화 | `NOT_STARTED` | 패키지 루트만 존재 | 각 기능 구현 |
| 로그·환경 설정 | `PARTIAL` | DB 비밀번호 환경변수화 완료, 로그 정책·프로필 미완료 | 공통 기반·품질 |
| Kafka 제외 | `COMPLETE` | 핵심 범위에서 제외 결정 | 필요 시 별도 실험 |

## 단계별 상태

| 단계 | 상태 | 결과 |
| ---: | --- | --- |
| 0. 목표·범위·회고 | `COMPLETE` | 리빌드 목적, 제외 범위와 학습 목표 확정 |
| 1. REST API 설계 | `COMPLETE` | `/api/v1` 기반 API 23개와 고정 DTO 계약 확정 |
| 2. ERD·테이블 설계 | `REVIEW` | ERDCloud 핵심 테이블 작성 완료, 저장소 문서화와 일부 보조 테이블 재검토 필요 |
| 3. Spring 프로젝트·Git | `COMPLETE` | Initializr, MySQL, Git, GitHub 연결과 Java 컴파일 완료 |
| 4. 공통 기반 | `IN_PROGRESS` | 기본 실행, Flyway, 오류 응답과 테스트 환경 구성 예정 |
| 5. 사용자·인증 | `IN_PROGRESS` | User Entity와 users 마이그레이션 작성·실행 완료, 회원가입 구현 예정 |
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
- `DB_PASSWORD`를 환경 변수로 주입한 `./gradlew bootRun --no-daemon` 실행 성공
- `http://localhost:8080` 요청에서 Spring Security 기본 응답 `401 Unauthorized` 확인
- 로컬 MySQL과 `DB_PASSWORD`를 사용하는 `./gradlew test --no-daemon` 성공
- `V1__create_users.sql` 적용과 Hibernate의 User Entity 스키마 검증 성공
- User Entity의 기본 필드, 생성시각 콜백과 회원가입 생성자 작성
- 회원가입 응답에 필요한 User의 ID, 이메일, 이름, 생성시각 조회 메서드 작성
- UserRepository의 Spring Data JPA 연결과 이메일 중복 조회 메서드 작성
- SignupRequest의 email, password, name 검증과 입력 정규화 작성
- SignupResponse의 userId, email, name, createdAt 응답 계약 작성
- SecurityConfig에 BCrypt PasswordEncoder Bean 등록
- 사용자 영속성·Repository·회원가입 DTO·PasswordEncoder를 포함한 `./gradlew test --no-daemon` 성공
- `flyway_schema_history`에서 V1 `create users`와 `success = 1` 확인
- MySQL `SHOW CREATE TABLE users`로 PK, UNIQUE, CHECK, InnoDB와 utf8mb4 구조 확인

## 아직 검증하지 않은 기반

- 테스트가 로컬 개발 DB에 의존하지 않도록 분리하는 방식
- ERDCloud 결과의 저장소 내 재현 가능한 문서 또는 DDL 변환

따라서 다음 작업은 별도 인증 세션 테이블 없이 User에 저장할 현재 Refresh Token 해시와 만료시각 컬럼을 결정하는 것입니다.

## 확정된 서비스 범위

- 이메일 기반 회원가입과 로그인
- 사용자에게 비밀번호 길이 8~64자를 요구하고 서버에서도 검증
- Access JWT 15분, 회전하는 Refresh Token 14일
- 한 사용자당 현재 Refresh Token 해시 하나만 저장
- Access Token은 로그아웃 후에도 남은 유효시간까지 유효하며 즉시 무효화하지 않음
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
- `refresh_token_hash VARCHAR(64) NULL UNIQUE`
- `refresh_token_expires_at DATETIME(6) NULL`
- Refresh Token 해시와 만료시각의 동시 NULL·동시 존재 CHECK 제약

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

### 인증 토큰 저장 결정

- 별도 `login_sessions`, `refresh_tokens` 테이블은 만들지 않음
- User에 현재 Refresh Token 해시와 만료시각만 저장
- 로그인·재발급 시 저장된 해시를 교체하고 로그아웃 시 제거
- Access Token 즉시 무효화와 Refresh Token 재사용 탐지는 현재 범위에서 제외

### 단계별 후보 테이블

- `naming_candidates`
- `concept_candidates`
- `story_candidates`
- `logo_candidates`
- 모두 `candidate_generations`를 참조하며 `(generation_id, display_order)` UNIQUE
- 활성 생성에는 순서 1~3의 후보가 존재

## ERD 재검토 항목

다음 항목은 구현 전에 누락 여부와 저장 방식을 확인합니다.

- Diagnosis의 복수 `coreValues`, `keywords`
- Concept 후보의 복수 `brandValues`
- Story 후보의 복수 `emotionalTones`
- FK 삭제 정책과 로컬 로고 파일 삭제 책임
- 주요 조회 인덱스와 후보 생성·선택 동시성 정책

이 항목들은 기존 설계를 부정하는 것이 아니라, ERDCloud에서 작성한 핵심 테이블을 실제 DDL로 옮기기 전 확인할 체크리스트입니다.

## 다음 작업 순서

1. User Entity에 회원가입 응답에 필요한 최소 조회 메서드를 추가한다.
2. UserRepository를 작성한다.
3. 회원가입 Request·Response DTO와 검증 규칙을 구현한다.
4. 회원가입 Service와 Controller를 작은 단위로 구현한다.
5. 작은 단위로 테스트하고 이 문서의 체크포인트를 갱신한다.

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
