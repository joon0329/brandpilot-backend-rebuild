# BrandPilot ERD and Table Design

> 상태: `IN_PROGRESS`  
> 기준 API: `API_SPEC.md 1.0.0`  
> 최종 수정일: `2026-07-16`

이 문서는 확정된 API 계약과 비즈니스 규칙을 MySQL 관계형 데이터 구조로 옮기는 Stage 2 설계 원본입니다. 테이블부터 임의로 만들지 않고 도메인 용어, 상태와 불변식을 먼저 정의한 뒤 물리 컬럼과 인덱스를 결정합니다.

## 1. Stage 2 설계 순서

1. 도메인 용어와 소유 관계 정의
2. Entity와 Value Object 후보 구분
3. 인증 세션 저장 구조 결정
4. 후보 생성 세트와 단계별 후보 저장 구조 결정
5. 논리 ERD 작성
6. 물리 테이블·컬럼·타입 작성
7. PK, FK, NOT NULL, UNIQUE 결정
8. 영구 삭제와 로컬 파일 생명주기 반영
9. 인덱스와 동시성 제어 결정
10. API와 ERD 매핑 검증

## 2. 도메인 용어 사전 초안

| 용어 | 의미 | 소유·생명주기 |
| --- | --- | --- |
| User | 이메일과 비밀번호로 로그인하는 사용자 | 여러 Brand를 소유 |
| Login Session | 한 사용자의 현재 활성 로그인과 `sid` | User당 하나의 활성 세션 |
| Refresh Token | Access Token 재발급용 회전 토큰 | Login Session에 종속 |
| Brand | 진단부터 최종 로고까지 이어지는 브랜드 컨설팅 작업 | User에 종속, 영구 삭제 가능 |
| Diagnosis | 사용자의 진단 원본 답변과 Fake 처리 결과 | Brand당 정확히 하나 |
| Stage | `NAMING`, `CONCEPT`, `STORY`, `LOGO`, `FINAL` 진행 위치 | Brand가 현재 위치를 보유 |
| Candidate Generation | 한 단계에서 한 번 생성한 후보 3개의 묶음과 상태 메타데이터 | Brand와 Stage에 종속, 단계별 최대 3개 |
| Candidate | 현재 선택 가능한 후보 또는 단계별 최종 선택 결과 | 재생성·선택 시 불필요한 행 삭제 |
| Selection | 단계에서 최종 확정한 후보 | 단계별 최대 하나 |
| Logo File Metadata | 로컬 로고 파일의 내부 경로, 형식과 크기 | Logo Candidate와 생명주기 공유 |

## 3. 브랜드 상태 전이

```text
진단 제출 성공
→ NAMING
→ CONCEPT
→ STORY
→ LOGO
→ FINAL
```

| 현재 단계 | 허용되는 완료 동작 | 다음 단계 |
| --- | --- | --- |
| `NAMING` | 최신 네이밍 후보 선택 | `CONCEPT` |
| `CONCEPT` | 최신 콘셉트 후보 선택 | `STORY` |
| `STORY` | 최신 스토리 후보 선택 | `LOGO` |
| `LOGO` | 최신 로고 후보 선택 | `FINAL` |
| `FINAL` | 결과 조회, 브랜드 영구 삭제 | 없음 |

브랜드 상태는 `IN_PROGRESS`, `COMPLETED`를 사용합니다. `FINAL`로 전이할 때만 `COMPLETED`가 되며 `completedAt`을 기록합니다.

## 4. 핵심 불변식 초안

1. Brand는 반드시 한 명의 User에게 속합니다.
2. 유효한 Brand에는 Diagnosis가 정확히 하나 존재합니다.
3. 한 User에는 현재 활성 Login Session이 최대 하나만 존재합니다.
4. 단계별 Candidate Generation은 최초 생성을 포함해 최대 3개입니다.
5. `ACTIVE` Candidate Generation에는 후보가 정확히 3개 존재합니다.
6. 한 Brand·Stage에서 선택 결과는 최대 하나입니다.
7. 선택 후보는 같은 Brand·Stage의 최신 활성 Generation에 속해야 합니다.
8. 이전 단계가 선택 완료되어야 다음 단계로 전이할 수 있습니다.
9. 완료된 단계의 선택은 변경할 수 없습니다.
10. `COMPLETED` Brand에는 네 단계의 선택 결과가 모두 존재해야 합니다.
11. Brand 영구 삭제 시 Diagnosis, Generation, Candidate와 로고 파일도 제거합니다.
12. 정규화된 이메일은 전체 User에서 유일합니다.
13. `SUPERSEDED` Generation에는 후보 내용을 남기지 않습니다.
14. `SELECTED` Generation에는 최종 선택 후보 하나만 남깁니다.

## 5. Entity 후보

### 인증 영역

- `User`
- `LoginSession`
- Refresh Token 회전 이력 저장 구조는 별도 테이블 여부를 검토합니다.

### 브랜드 영역

- `Brand`
- `Diagnosis`
- `CandidateGeneration`
- `NamingCandidate`
- `ConceptCandidate`
- `StoryCandidate`
- `LogoCandidate`

후보를 하나의 범용 JSON 테이블에 저장하면 기존 `Map<String, Object>` 구조와 비슷한 문제가 DB로 이동합니다. 단계별 후보의 필드가 다르므로 관계형 컬럼으로 표현하는 방향을 우선 검토합니다.

## 6. 첫 번째 설계 결정

후보 생성과 후보 테이블 구조를 다음 대안 중에서 결정합니다.

| 대안 | 구조 | 장점 | 단점 |
| --- | --- | --- | --- |
| A | 공통 Generation + 공통 Candidate JSON | 테이블 수가 적음 | 타입·제약조건과 조회가 불명확 |
| B | 단계별 Generation + 단계별 Candidate | DB 제약이 가장 명확 | 비슷한 테이블이 8개로 증가 |
| C | 공통 Generation + 단계별 Candidate | 생성 정책 공통화와 타입 컬럼을 함께 확보 | Generation의 Stage와 후보 테이블 일치 검증 필요 |

현재 채택안은 `C`입니다. 공통 `candidate_generations`에서 생성 회차와 `ACTIVE`, `SUPERSEDED`, `SELECTED` 상태를 관리하고 단계별 후보는 명시적인 관계형 컬럼으로 저장합니다.

## 7. 아직 확정하지 않은 물리 설계

- PK를 `BIGINT AUTO_INCREMENT`로 통일할지 여부
- 시간 저장 기준과 `DATETIME(6)` 사용 여부
- Enum을 MySQL ENUM 또는 `VARCHAR`로 저장할지 여부
- 후보 선택을 Brand FK 컬럼으로 둘지 별도 Selection 테이블로 둘지 여부
- Refresh Token 회전·재사용 탐지를 위한 이력 테이블 구조
- 낙관적 락용 `version` 컬럼 위치
- 영구 삭제 시 DB Cascade와 애플리케이션 삭제 책임 분담
- 주요 복합 인덱스

## 8. Stage 2 완료 조건

- [ ] 도메인 용어와 Entity 경계 확정
- [ ] 상태 전이와 불변식 확정
- [ ] Mermaid 논리 ERD 작성
- [ ] 물리 테이블과 컬럼 타입 작성
- [ ] PK·FK·NOT NULL·UNIQUE 작성
- [ ] 삭제·인덱스·동시성 정책 작성
- [ ] API 23개와 데이터 출처 매핑 완료
