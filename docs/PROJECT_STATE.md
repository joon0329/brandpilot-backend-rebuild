# BrandPilot Backend Rebuild - Current State

이 문서는 현재 어디까지 진행했는지와 다음에 무엇을 할지만 관리합니다. 프로젝트 목적·학습 방식·로드맵은 `PROJECT_PLAN.md`, API 필드는 `API_SPEC.md`, 테이블 컬럼은 `ERD_DESIGN.md`를 확인합니다.

## 현재 체크포인트

```yaml
last_updated: 2026-07-18
current_milestone: 회원가입 수직 기능
durable_checkpoint: SIGNUP_SERVICE_UNIT_TESTED
working_checkpoint: SIGNUP_SERVICE_UNIT_TESTED
next_action: DUPLICATE_EMAIL을 409 공통 오류 응답으로 변환하는 최소 예외 처리 기반을 만든다
```

- `durable_checkpoint`: 현재 `origin/main`에서 다시 시작할 수 있는 마지막 커밋 기준 상태
- `working_checkpoint`: 로컬 작업 트리에서 검증했지만 아직 커밋되지 않을 수 있는 상태
- 실제 미커밋 파일은 이 문서에 복사하지 않고 매번 `git status`로 확인합니다.

## 마지막으로 확인된 Git 상태

- 브랜치: `main`, `origin/main` 추적
- 마지막 커밋: `887f25a feat: implement signup service`
- `42836cc`에서 문서 구조 리팩터링을 커밋했습니다.
- `887f25a`에서 SignupService, DuplicateEmailException과 SignupServiceTest를 커밋하고 `origin/main`에 push했습니다.

이 항목은 세션 시작 시 실제 `git status`와 다르면 실제 Git 상태를 우선합니다.

## 현재 기능: 회원가입

- 기준 계약: `API_SPEC.md`의 `8.1 회원가입`
- 기준 데이터: `ERD_DESIGN.md`의 `users`

### 완료 및 검증

- [x] Flyway `V1__create_users.sql` 작성
- [x] 로컬 MySQL에서 V1 적용, `flyway_schema_history`와 실제 `users` 스키마 확인
- [x] User Entity와 users 스키마에 대한 Hibernate validate 확인
- [x] UserRepository와 `existsByEmail` 작성
- [x] SignupRequest·SignupResponse와 Bean Validation 작성
- [x] BCrypt PasswordEncoder Bean 작성
- [x] SignupService의 이메일 중복 확인, 비밀번호 해싱, 저장과 응답 변환 작성
- [x] SignupService 정상 가입 단위 테스트 작성
- [x] SignupService 중복 이메일 단위 테스트 작성
- [x] 2026-07-18 `./gradlew test --tests com.brandpilot.backend.user.SignupServiceTest --no-daemon` 성공

### 다음 TODO

1. [ ] 공통 오류 응답 DTO와 오류 코드의 최소 구조를 결정한다.
2. [ ] `DuplicateEmailException`을 `409 DUPLICATE_EMAIL`로 변환한다.
3. [ ] SignupController와 `POST /api/v1/users`를 연결한다.
4. [ ] 회원가입 API를 공개하고 나머지 요청은 인증하도록 SecurityFilterChain을 설정한다.
5. [ ] Controller 테스트로 201·400·409 응답을 검증한다.
6. [ ] 실제 MySQL에서 HTTP 요청부터 비밀번호 해시 저장까지 통합 검증한다.
7. [ ] 동시에 같은 이메일로 가입할 때 DB UNIQUE 충돌도 `DUPLICATE_EMAIL`로 변환되는지 검증한다.

한 번에 위 목록 전체를 구현하지 않고 TODO 하나씩 진행합니다.

## 전체 단계 현황

| 단계 | 상태 | 완료 판단 |
| ---: | --- | --- |
| 0. 목적·범위·회고 | `COMPLETE` | `PROJECT_PLAN.md`에 통합 |
| 1. REST API 계약 | `COMPLETE` | API Spec 1.3.0 |
| 2. ERD·데이터 설계 | `IN_PROGRESS` | ERDCloud 핵심 테이블 문서화 완료, 복수 값·삭제·동시성 결정 미완료 |
| 3. 프로젝트·MySQL·Git | `COMPLETE` | 기본 실행, DB와 GitHub 연결 확인 |
| 4. 공통 기반 | `IN_PROGRESS` | V1 Flyway 완료, 공통 오류와 독립 테스트 환경 미완료 |
| 5. 사용자·인증 | `IN_PROGRESS` | 회원가입 Service 단위 테스트 완료, HTTP·Security·로그인 미완료 |
| 6. 브랜드 워크플로 | `NOT_STARTED` | 진단부터 최종 결과 |
| 7. 로컬 파일 저장 | `NOT_STARTED` | 로고 파일과 보상 처리 |
| 8. 품질·안정성 | `NOT_STARTED` | 동시성·쿼리·회귀 테스트 |
| 9. 면접 대비·완료 | `NOT_STARTED` | 설명·트러블슈팅·최종 검증 |

Stage 4의 공통 기능은 과도하게 미리 만들지 않고 Stage 5의 첫 수직 기능인 회원가입에서 필요한 만큼 구현합니다. 따라서 현재 단계 표에서 Stage 4와 Stage 5가 동시에 `IN_PROGRESS`인 것은 의도된 상태입니다.

## 검증이 남은 항목

- 전체 테스트를 로컬 개발 DB와 `DB_PASSWORD` 없이 실행하는 테스트 환경 전략
- 회원가입 HTTP 요청의 Bean Validation과 201·400·409 응답
- 실제 MySQL 트랜잭션, 비밀번호 해시와 생성시각 저장
- DB UNIQUE 충돌의 공통 오류 변환
- ERD의 복수 값, 삭제 정책과 동시성 미결정 사항 확정

## 결정이 필요한 사항

### 단일 기기 로그인 의미

현재 API·ERD는 사용자당 Refresh Token 해시 하나만 저장합니다. 새 로그인 후 기존 Refresh Token은 사용할 수 없지만 기존 Access Token은 최대 15분 동안 유효합니다.

다음 중 하나를 JWT 구현 전에 선택해야 합니다.

1. 실용적 단일 Refresh 세션: 기존 Access Token의 최대 15분 중복 사용을 허용
2. 엄격한 단일 기기: `sid` 또는 `tokenVersion`을 요청마다 확인해 이전 Access Token도 거부

현재 문서는 1번 방식으로 작성되어 있지만, 사용자가 원래 말한 “한 기기에서만 사용”과 정확히 같은지 다시 확인해야 합니다.

### ERD 미결정 사항

- Diagnosis의 복수 `coreValues`, `keywords` 저장 구조
- Concept 후보의 복수 `brandValues` 저장 구조
- Story 후보의 복수 `emotionalTones` 저장 구조
- User와 Brand 사이 삭제 정책
- 후보 생성·선택의 최종 동시성 정책과 인덱스
- 로고 파일 삭제 실패 시 보상·재시도 정책

## 다음 커밋 체크포인트

다음 커밋 후보는 공통 오류 응답의 최소 구조와 `DuplicateEmailException`의 `409 DUPLICATE_EMAIL` 변환을 구현하고 좁은 테스트로 검증한 기능 단위입니다.

추천 메시지:

```text
feat: add signup error handling
```

## 다음 Codex 세션 시작 문장

```text
PROJECT_PLAN.md와 PROJECT_STATE.md를 먼저 읽고 git status와 실제 코드를 대조해줘. API_SPEC.md의 회원가입 오류 계약도 확인해줘. 완성 코드는 작성하지 말고 공통 오류 응답과 DUPLICATE_EMAIL 변환에 필요한 전체 TODO를 준비한 뒤 TODO 1만 기초부터 설명해줘.
```
