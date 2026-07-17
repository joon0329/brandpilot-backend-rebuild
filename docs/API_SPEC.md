# BrandPilot Backend API Specification

> 상태: `STAGE_1_COMPLETE`  
> 버전: `1.1.0`  
> 최종 수정일: `2026-07-16`

이 문서는 BrandPilot Backend Rebuild의 REST API 설계 원본입니다. 구현 전에 API 계약을 정의하고, 구현 이후에는 Swagger/OpenAPI 및 테스트 결과와 일치하도록 관리합니다.

## 1. API 설계 목표

- 브랜드 컨설팅의 단계 순서를 서버에서 보장합니다.
- 인증된 사용자만 자신의 브랜드를 생성·변경할 수 있습니다.
- 요청과 응답을 명확한 DTO로 정의합니다.
- 정상 흐름과 실패 흐름을 함께 설계합니다.
- 적절한 HTTP Method와 상태 코드를 사용합니다.
- 실제 AI 없이도 Fake 후보 생성기로 전체 흐름을 검증할 수 있게 합니다.

## 2. 서비스 범위

### 서비스 정의

BrandPilot은 브랜드 구축 경험이 부족한 예비 창업자와 초기 기업 담당자가 단계별 질문에 답하고, 시스템이 생성한 후보를 선택하여 이름·컨셉·스토리·로고로 구성된 하나의 브랜드를 완성하고 관리할 수 있게 하는 백엔드 서비스입니다.

### 사용자

| 사용자 | 가능한 작업 |
| --- | --- |
| 비회원 | 회원가입, 로그인 |
| 로그인 사용자 | 자신의 브랜드 생성·조회·진행·삭제 |

관리자 기능은 현재 범위에서 제외합니다.

### 포함

- 회원가입
- 로그인과 인증
- 브랜드 생성 및 조회
- 기업 진단
- 네이밍 후보 생성 및 선택
- 컨셉 후보 생성 및 선택
- 스토리 후보 생성 및 선택
- 로고 후보 생성 및 선택
- 완성된 브랜드 결과 조회
- 브랜드 삭제
- 로고 파일의 로컬 저장

### 제외

- 프론트엔드
- 실제 AI 서버 및 LLM 호출
- 투자 라운지·게시판
- AWS와 S3
- Kafka
- 사용자 프로필 조회·수정
- 비밀번호 변경·찾기·재설정
- 이메일 인증
- 회원 탈퇴

## 3. 핵심 사용자 흐름

```text
회원가입
→ 로그인
→ 기업 진단 제출과 브랜드 생성
→ 네이밍 후보 생성
→ 네이밍 선택
→ 컨셉 후보 생성
→ 컨셉 선택
→ 스토리 후보 생성
→ 스토리 선택
→ 로고 후보 생성
→ 로고 선택
→ 최종 브랜드 결과 조회
```

### 중단 후 재개 흐름

사용자는 모든 단계를 한 번의 접속에서 완료하지 않아도 됩니다. 서버는 단계 순서를 계속 강제하되, 저장된 `currentStep`부터 작업을 재개할 수 있게 합니다.

```text
내 브랜드 목록 조회
→ 브랜드 상세에서 currentStep 확인
→ 현재 단계 조회 API로 최신 후보 확인
→ 활성 후보가 있으면 선택부터 재개
→ 활성 후보가 없으면 후보 생성부터 재개
```

- 완료된 이전 단계는 건너뛰거나 다시 선택할 수 없습니다.
- 잠겨 있는 미래 단계에는 접근할 수 없습니다.
- 현재 단계의 최신 활성 후보만 다시 불러옵니다.
- 페이지 새로고침이나 접속 종료로 후보를 다시 생성하지 않아도 됩니다.

### 브랜드 생성 시점

기업 진단 제출이 성공한 시점에 브랜드를 생성합니다.

```text
POST /api/v1/brands + 진단 답변
→ 진단 처리
→ Brand와 진단 결과 저장
→ brandId 발급
→ 다음 단계 NAMING
```

BrandPilot의 브랜드는 진단부터 로고까지 이어지는 하나의 컨설팅 결과입니다. 진단 전에는 독립적으로 관리할 브랜드 데이터가 없으므로 빈 브랜드나 인위적인 작업명을 만들지 않습니다. 진단 처리에 실패하면 브랜드도 생성하지 않습니다. 추후 진단 답변 임시 저장이 필요해지면 `Brand`와 구분되는 Draft 개념을 별도로 설계합니다.

## 4. 공통 규칙

### 4.1 Base URL

```text
/api/v1
```

첫 번째 공개 API 계약을 `/api/v1`로 제공합니다. 기존 클라이언트와 호환되지 않는 변경이 필요할 때 새 버전을 검토합니다.

### 4.2 Content-Type

일반적인 요청과 응답은 JSON을 사용합니다.

```http
Content-Type: application/json
```

### 4.3 인증 헤더

JWT Access Token으로 API 요청을 인증합니다.

```http
Authorization: Bearer {accessToken}
```

- Access Token은 짧은 유효시간을 가진 JWT로 발급합니다.
- Refresh Token은 Access Token 재발급에 사용합니다.
- Refresh Token은 서버에서 폐기할 수 있도록 해시하여 저장합니다.
- Access Token의 Payload에는 비밀번호와 불필요한 개인정보를 넣지 않습니다.
- Access Token 유효시간은 15분입니다.
- Refresh Token 유효시간은 14일입니다.
- Refresh Token은 예측 불가능한 Opaque Token으로 발급합니다.
- Refresh Token은 HttpOnly Cookie로 전달하고 사용할 때마다 회전합니다.
- 운영 Cookie에는 `Secure`, `HttpOnly`, `SameSite=Strict`를 적용합니다.
- 한 사용자에게 하나의 활성 로그인 세션만 허용합니다.
- 새 로그인 성공 시 기존 로그인 세션을 폐기하고 새 로그인으로 대체합니다.
- Access Token의 `sid`를 DB의 활성 Session ID와 비교해 기존 로그인을 즉시 무효화합니다.

### 4.4 시간 형식

응답의 날짜와 시간은 ISO 8601 형식을 사용합니다.

```text
2026-07-14T15:30:00+09:00
```

서버와 DB의 시간대 저장 기준은 ERD·공통 설정 단계에서 결정합니다.

### 4.5 ID 형식

공개 API의 리소스 ID는 1 이상의 JSON 정수로 표현합니다. MySQL의 실제 PK 타입과 생성 전략은 Stage 2 ERD에서 확정합니다.

### 4.6 응답 필드 표기법

JSON 필드는 `camelCase`를 사용합니다.

```json
{
  "brandId": 1,
  "currentStep": "NAMING",
  "createdAt": "2026-07-14T15:30:00+09:00"
}
```

## 5. HTTP 상태 코드

| Status | 사용 상황 |
| ---: | --- |
| `200 OK` | 조회·수정·명령 처리 성공 및 응답 본문 존재 |
| `201 Created` | 회원, 브랜드 등 새로운 리소스 생성 |
| `204 No Content` | 삭제 성공 또는 응답 본문이 필요 없는 처리 |
| `400 Bad Request` | JSON 형식 오류, 입력값 검증 실패 |
| `401 Unauthorized` | 인증 정보 없음, 토큰 만료·변조 |
| `403 Forbidden` | 인증됐지만 해당 리소스에 접근 권한 없음 |
| `404 Not Found` | 사용자 또는 브랜드 등 리소스 없음 |
| `409 Conflict` | 중복 가입 정보, 잘못된 단계 전이, 동시 수정 충돌 |
| `415 Unsupported Media Type` | 지원하지 않는 Content-Type |
| `500 Internal Server Error` | 예상하지 못한 서버 내부 오류 |

## 6. 공통 오류 응답

모든 API 오류는 동일한 구조를 사용합니다.

```json
{
  "timestamp": "2026-07-14T15:30:00+09:00",
  "status": 409,
  "code": "BRAND_STEP_CONFLICT",
  "message": "현재 브랜드 단계에서는 요청한 작업을 수행할 수 없습니다.",
  "path": "/api/v1/brands/1/naming/candidates",
  "fieldErrors": []
}
```

입력값 검증에 실패하면 `fieldErrors`를 반환합니다.

```json
{
  "timestamp": "2026-07-14T15:30:00+09:00",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "요청 값이 올바르지 않습니다.",
  "path": "/api/v1/users",
  "fieldErrors": [
    {
      "field": "email",
      "reason": "올바른 이메일 형식이어야 합니다."
    }
  ]
}
```

- 검증 오류가 아니면 `fieldErrors`는 빈 배열입니다.
- 비밀번호, 토큰, SQL, 스택 트레이스와 내부 파일 경로는 오류 응답에 포함하지 않습니다.
- 예상하지 못한 오류는 외부에 일반화된 메시지를 반환하고 상세 원인은 서버 로그에만 기록합니다.

## 7. API 전체 목록

아래 목록은 Stage 1에서 확정한 BrandPilot Backend API입니다. 상세 계약을 변경하면 이 목록과 변경 이력을 함께 갱신합니다.

### 7.1 인증과 사용자

| Method | Endpoint | 인증 | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/users` | 불필요 | 이메일 기반 회원가입 | 확정 |
| `POST` | `/api/v1/auth/tokens` | 불필요 | 이메일·비밀번호 검증 후 토큰 발급 | 확정 |
| `POST` | `/api/v1/auth/tokens/refresh` | Refresh Token Cookie | Access Token 재발급 및 Refresh Token 회전 | 확정 |
| `DELETE` | `/api/v1/auth/tokens` | Refresh Token Cookie | 현재 Refresh Token 폐기 및 로그아웃 | 확정 |

### 7.2 브랜드

| Method | Endpoint | 인증 | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/brands` | Access Token | 진단 답변 제출, 진단 처리 및 브랜드 생성 | 확정 |
| `GET` | `/api/v1/brands` | Access Token | 내 브랜드 목록 조회 | 확정 |
| `GET` | `/api/v1/brands/{brandId}` | 브랜드 소유자 | 브랜드 기본 정보와 단계별 진행 상태 조회 | 확정 |
| `DELETE` | `/api/v1/brands/{brandId}` | 브랜드 소유자 | 브랜드와 모든 하위 데이터 영구 삭제 | 확정 |

### 7.3 기업 진단

| Method | Endpoint | 인증 | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/brands/{brandId}/diagnosis` | 브랜드 소유자 | 진단 원본 답변과 처리 결과 조회 | 확정 |

### 7.4 네이밍

| Method | Endpoint | 인증 | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/brands/{brandId}/naming/candidates` | 브랜드 소유자 | 네이밍 후보 생성 | 확정 |
| `PUT` | `/api/v1/brands/{brandId}/naming/selection` | 브랜드 소유자 | 네이밍 최종 선택 | 확정 |
| `GET` | `/api/v1/brands/{brandId}/naming` | 브랜드 소유자 | 최신 네이밍 후보·선택 결과 조회 | 확정 |

### 7.5 컨셉

| Method | Endpoint | 인증 | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/brands/{brandId}/concept/candidates` | 브랜드 소유자 | 컨셉 후보 생성 | 확정 |
| `PUT` | `/api/v1/brands/{brandId}/concept/selection` | 브랜드 소유자 | 컨셉 최종 선택 | 확정 |
| `GET` | `/api/v1/brands/{brandId}/concept` | 브랜드 소유자 | 최신 컨셉 후보·선택 결과 조회 | 확정 |

### 7.6 스토리

| Method | Endpoint | 인증 | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/brands/{brandId}/story/candidates` | 브랜드 소유자 | 스토리 후보 생성 | 확정 |
| `PUT` | `/api/v1/brands/{brandId}/story/selection` | 브랜드 소유자 | 스토리 최종 선택 | 확정 |
| `GET` | `/api/v1/brands/{brandId}/story` | 브랜드 소유자 | 최신 스토리 후보·선택 결과 조회 | 확정 |

### 7.7 로고

| Method | Endpoint | 인증 | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/brands/{brandId}/logo/candidates` | 브랜드 소유자 | 로고 후보 생성 및 로컬 파일 저장 | 확정 |
| `PUT` | `/api/v1/brands/{brandId}/logo/selection` | 브랜드 소유자 | 로고 최종 선택 및 브랜드 완료 | 확정 |
| `GET` | `/api/v1/brands/{brandId}/logo` | 브랜드 소유자 | 최신 로고 후보·선택 결과 조회 | 확정 |

### 7.8 로고 이미지

| Method | Endpoint | 인증 | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/brands/{brandId}/logo/candidates/{candidateId}/image` | 브랜드 소유자 | 후보에 연결된 로컬 이미지 조회 | 확정 |

### 7.9 최종 결과

| Method | Endpoint | 인증 | 설명 | 상태 |
| --- | --- | --- | --- | --- |
| `GET` | `/api/v1/brands/{brandId}/result` | 브랜드 소유자 | 완성된 브랜드 최종 결과 조회 | 확정 |

## 8. 상세 API 명세

### 8.1 회원가입

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `POST` |
| Endpoint | `/api/v1/users` |
| 인증 | 불필요 |
| 권한 | 없음 |
| 설명 | 이메일을 로그인 식별자로 사용하는 사용자를 생성합니다. |
| 멱등성 | 멱등하지 않음. 같은 이메일의 반복 요청은 충돌로 처리합니다. |

#### Request Body

```json
{
  "email": "user@example.com",
  "password": "correct horse battery staple",
  "name": "홍길동"
}
```

`loginId`와 `mobileNumber`는 현재 서비스 요구사항에 필요하지 않으므로 제외합니다.

#### 필드 정책

| 필드 | 타입 | 필수 | 정책 |
| --- | --- | --- | --- |
| `email` | String | 예 | 이메일 형식, 앞뒤 공백 제거, 소문자 정규화, 최대 254자 |
| `password` | String | 예 | 8~64자, 문자 조합 강제 없음, 응답 및 로그에서 제외 |
| `name` | String | 예 | 앞뒤 공백 제거, 공백만 입력 불가, 1~50자 |

#### Success Response

```http
HTTP/1.1 201 Created
Content-Type: application/json
```

```json
{
  "userId": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "createdAt": "2026-07-14T15:30:00+09:00"
}
```

비밀번호와 비밀번호 해시는 응답에 포함하지 않습니다.

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `400` | `VALIDATION_FAILED` | 이메일, 비밀번호 또는 이름 검증 실패 |
| `409` | `DUPLICATE_EMAIL` | 이미 가입된 이메일 |

#### 비즈니스 규칙

- 이메일은 한 사용자만 사용할 수 있습니다.
- 이메일 중복은 애플리케이션 검사와 DB UNIQUE 제약으로 함께 방지합니다.
- 비밀번호는 평문으로 저장하거나 로그에 기록하지 않습니다.
- 성공 시 생성된 사용자 위치를 `Location` 헤더로 반환합니다.
- 사용자 역할은 클라이언트가 지정하지 않고 서버 기본값으로 설정합니다.
- 비밀번호는 8자 이상 64자 이하로 제한하고 대문자·숫자·특수문자 조합을 강제하지 않습니다.

#### 설계 메모

- 회원가입은 인증 명령보다 사용자 리소스 생성으로 보아 `POST /users`를 사용합니다.
- 별도의 `loginId` 없이 이메일을 로그인 식별자로 사용합니다.
- 휴대전화 번호는 현재 유스케이스가 없으므로 받지 않습니다.

### 8.2 로그인 및 토큰 발급

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `POST` |
| Endpoint | `/api/v1/auth/tokens` |
| 인증 | 불필요 |
| 권한 | 없음 |
| 설명 | 이메일과 비밀번호를 검증하고 Access Token과 Refresh Token을 발급합니다. |
| 멱등성 | 멱등하지 않음. 호출할 때마다 새로운 토큰 쌍을 발급합니다. |

#### Request Body

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

#### Validation

| 필드 | 규칙 | 실패 코드 |
| --- | --- | --- |
| `email` | 필수, 이메일 형식, 앞뒤 공백 제거, 소문자 정규화 | `VALIDATION_FAILED` |
| `password` | 필수, 빈 문자열 불가 | `VALIDATION_FAILED` |

로그인 요청에서는 회원가입 정책인 비밀번호 8~64자를 다시 강제하지 않고 필수값 여부만 우선 검증합니다. 저장된 기존 비밀번호와 일치하는지는 인증 과정에서 확인합니다.

#### Success Response

```http
HTTP/1.1 200 OK
Cache-Control: no-store
Pragma: no-cache
Set-Cookie: refreshToken={token}; Max-Age=1209600; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth
```

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJ...",
  "accessTokenExpiresIn": 900
}
```

Access Token은 JSON 본문으로 반환하고 Refresh Token은 Cookie로만 전달합니다. 개발 환경의 HTTP에서는 `Secure` 적용을 환경 설정으로 분리하고, 운영 환경에서는 반드시 활성화합니다.

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `400` | `VALIDATION_FAILED` | 이메일 또는 비밀번호 누락, 이메일 형식 오류 |
| `401` | `INVALID_CREDENTIALS` | 존재하지 않는 이메일 또는 비밀번호 불일치 |

#### 비즈니스 규칙

- 이메일 존재 여부와 비밀번호 불일치를 외부에서 구분하지 않고 모두 `INVALID_CREDENTIALS`로 반환합니다.
- 로그인 성공 시 기존 비밀번호나 비밀번호 해시를 응답하지 않습니다.
- Access Token과 Refresh Token은 로그에 기록하지 않습니다.
- Refresh Token은 원문이 아닌 해시를 서버에 저장합니다.
- 토큰 응답이 브라우저나 중간 캐시에 저장되지 않도록 캐시 방지 헤더를 반환합니다.

#### 설계 메모

- 로그인이라는 동작명 대신 인증 토큰 리소스를 생성한다는 의미로 `POST /auth/tokens`를 사용합니다.
- 로그인 성공은 사용자 리소스 생성이 아니라 인증 정보 발급이므로 `200 OK`를 사용합니다.
- 한 사용자에게 하나의 활성 로그인만 허용합니다.
- 새 로그인 성공 시 기존 로그인 세션을 폐기하고 새 로그인으로 대체합니다.
- Access Token의 `sid`와 DB의 활성 Session ID를 비교해 기존 Access Token을 즉시 무효화합니다.

#### 구현 단계에서 다시 학습할 내용

- JWT Header, Payload, Signature
- Spring Security Filter Chain
- Access Token 생성과 검증
- `sid` Claim과 활성 로그인 세션 조회
- Refresh Token 원문·해시 비교
- Refresh Token Rotation과 재사용 탐지
- 로그인·재발급 동시 요청 처리
- 인증 실패와 인가 실패 구분

### 8.3 회원 부가 API 제외

현재 사용자 요구사항은 회원가입과 로그인뿐입니다. 사용자 프로필 조회·수정, 비밀번호 변경·재설정, 이메일 인증과 회원 탈퇴 API는 제공하지 않습니다.

Access Token 재발급과 로그아웃은 별도의 회원 기능이 아니라 앞서 채택한 JWT 로그인 수명주기를 안전하게 관리하기 위한 인증 API이므로 유지합니다.

### 8.4 Access Token 재발급

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `POST` |
| Endpoint | `/api/v1/auth/tokens/refresh` |
| 인증 | Refresh Token Cookie |
| 권한 | 유효한 Refresh Token 소유자 |
| 설명 | Access Token을 재발급하고 Refresh Token을 새 값으로 회전합니다. |
| 멱등성 | 멱등하지 않음. 성공할 때마다 새로운 토큰 쌍이 발급됩니다. |

#### Request

요청 본문은 없습니다. Cookie에서 Refresh Token을 읽습니다.

```http
Cookie: refreshToken={token}
```

#### Success Response

```http
HTTP/1.1 200 OK
Cache-Control: no-store
Pragma: no-cache
Set-Cookie: refreshToken={newToken}; Max-Age=1209600; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth
```

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJ...",
  "accessTokenExpiresIn": 900
}
```

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `401` | `REFRESH_TOKEN_REQUIRED` | Refresh Token Cookie 없음 |
| `401` | `INVALID_REFRESH_TOKEN` | 토큰 형식 또는 해시 불일치 |
| `401` | `EXPIRED_REFRESH_TOKEN` | Refresh Token 만료 |
| `401` | `REVOKED_REFRESH_TOKEN` | 로그아웃 등으로 이미 폐기됨 |
| `401` | `REUSED_REFRESH_TOKEN` | 회전되어 이미 사용된 토큰 재사용 탐지 |

#### 비즈니스 규칙

- Refresh Token 원문을 해시하여 저장된 값과 비교합니다.
- 유효한 Refresh Token은 한 번 사용한 즉시 폐기합니다.
- 새로운 Access Token과 Refresh Token을 함께 발급합니다.
- 이미 사용된 Refresh Token이 다시 제출되면 재사용으로 처리합니다.
- 재사용 탐지 시 같은 로그인 계열의 토큰을 모두 폐기하는 정책을 적용할 수 있으며, 정확한 범위는 ERD·동시성 설계에서 확정합니다.

### 8.5 로그아웃

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `DELETE` |
| Endpoint | `/api/v1/auth/tokens` |
| 인증 | Refresh Token Cookie |
| 권한 | 현재 로그인 사용자 |
| 설명 | 현재 Refresh Token을 폐기하고 Cookie를 제거합니다. |
| 멱등성 | 멱등함. 반복 호출해도 로그아웃 상태는 동일합니다. |

#### Request

요청 본문은 없습니다.

```http
Cookie: refreshToken={token}
```

#### Success Response

```http
HTTP/1.1 204 No Content
Set-Cookie: refreshToken=; Max-Age=0; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth
Clear-Site-Data: "cache"
```

#### 비즈니스 규칙

- 서버에 저장된 현재 Refresh Token을 폐기합니다.
- 브라우저의 Refresh Token Cookie를 만료시킵니다.
- Cookie가 없거나 이미 폐기됐더라도 `204 No Content`를 반환해 반복 호출을 허용합니다.
- 유효한 Refresh Token으로 활성 로그인 세션을 폐기하면, 같은 `sid`를 가진 Access Token도 다음 요청부터 거부됩니다.
- Cookie가 없어 서버에서 폐기할 로그인 세션을 식별할 수 없는 경우에는 클라이언트 Cookie만 정리하고 `204 No Content`를 반환합니다.

### 8.6 진단 제출과 브랜드 생성

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `POST` |
| Endpoint | `/api/v1/brands` |
| 인증 | Access Token 필요 |
| 권한 | 현재 인증 사용자 |
| 설명 | 진단 답변을 처리하고 유효한 브랜드 컨설팅을 생성합니다. |
| 멱등성 | 멱등하지 않음. 같은 요청을 반복하면 새로운 브랜드가 생성될 수 있습니다. |

#### Request Body

```json
{
  "diagnosisAnswers": {
    "industry": "반려동물 서비스",
    "businessDescription": "검증된 파트너가 반려견 산책을 대신하는 매칭 서비스",
    "targetCustomer": "산책 시간이 부족한 20~40대 반려인",
    "customerProblem": "바쁜 일정 때문에 반려견에게 충분한 산책을 제공하기 어렵다",
    "coreValues": [
      "신뢰",
      "안전",
      "따뜻함"
    ],
    "differentiation": "신원과 경력을 검증한 산책 파트너만 활동한다",
    "desiredImage": "따뜻하지만 전문적이고 믿을 수 있는 이미지"
  }
}
```

#### 필드 정책

| 필드 | 타입 | 필수 | 정책 |
| --- | --- | --- | --- |
| `diagnosisAnswers` | Object | 예 | 진단 답변 객체 |
| `industry` | String | 예 | 공백 제거, 2~100자 |
| `businessDescription` | String | 예 | 공백 제거, 10~1,000자 |
| `targetCustomer` | String | 예 | 공백 제거, 5~500자 |
| `customerProblem` | String | 예 | 공백 제거, 10~1,000자 |
| `coreValues` | String Array | 예 | 1~5개, 각 항목 공백 제거 및 1~30자, 중복 불가 |
| `differentiation` | String | 예 | 공백 제거, 10~1,000자 |
| `desiredImage` | String | 예 | 공백 제거, 5~500자 |

#### DTO 구조

```text
BrandCreateRequest
└── DiagnosisAnswersRequest diagnosisAnswers
    ├── String industry
    ├── String businessDescription
    ├── String targetCustomer
    ├── String customerProblem
    ├── List<String> coreValues
    ├── String differentiation
    └── String desiredImage
```

공개 API에서 `Map<String, Object>`를 사용하지 않습니다. 고정 Request DTO를 사용하고, 질문이 변경되면 DTO와 API 버전을 의식적으로 변경합니다.

#### Success Response

```http
HTTP/1.1 201 Created
Location: /api/v1/brands/1
```

```json
{
  "brandId": 1,
  "currentStep": "NAMING",
  "status": "IN_PROGRESS",
  "diagnosis": {
    "summary": "신뢰와 안전을 중심으로 한 반려동물 서비스",
    "keywords": [
      "신뢰",
      "안전",
      "따뜻함"
    ]
  },
  "createdAt": "2026-07-14T15:30:00+09:00"
}
```

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `400` | `VALIDATION_FAILED` | 진단 답변 누락 또는 필드 검증 실패 |
| `401` | `AUTHENTICATION_REQUIRED` | Access Token 없음 |
| `401` | `INVALID_TOKEN` | Access Token 검증 실패 |
| `401` | `LOGIN_SESSION_REVOKED` | 현재 활성 로그인이 아님 |
| `500` | `DIAGNOSIS_PROCESSING_FAILED` | 진단 처리 중 예상하지 못한 오류 |

#### 비즈니스 규칙

- 인증 사용자만 브랜드를 생성할 수 있습니다.
- 생성된 브랜드의 소유자는 인증 사용자로 설정합니다.
- 진단 처리와 저장이 모두 성공해야 브랜드가 생성됩니다.
- 진단 결과와 Brand 저장은 하나의 일관된 작업으로 처리합니다.
- 생성 직후 `currentStep`은 `NAMING`, `status`는 `IN_PROGRESS`입니다.
- 클라이언트가 소유자 ID, 현재 단계 또는 상태를 직접 지정할 수 없습니다.

#### 설계 메모

- 진단은 BrandPilot 브랜드 컨설팅의 필수 시작 단계이므로 브랜드 생성 요청에 포함합니다.
- 진단 전 임시 저장 요구사항이 생기면 Brand를 미리 만들지 않고 별도 Draft 리소스를 검토합니다.
- 실제 외부 AI를 연결할 경우 느린 외부 호출을 DB 트랜잭션 안에 포함하지 않도록 처리 경계를 다시 설계합니다.
- 중복 제출로 여러 브랜드가 생성되는 문제는 Idempotency Key 도입 필요성과 함께 추후 검토합니다.

### 8.6.1 진단 원본 답변과 처리 결과 조회

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `GET` |
| Endpoint | `/api/v1/brands/{brandId}/diagnosis` |
| 인증 | Access Token 필요 |
| 권한 | 브랜드 소유자 |
| 설명 | 브랜드 생성 시 저장한 진단 원본 답변과 처리 결과를 조회합니다. |
| 멱등성 | 멱등 |

#### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `brandId` | Long | 예 | 진단을 조회할 브랜드 ID |

#### Success Response

```http
HTTP/1.1 200 OK
Cache-Control: private, no-store
```

```json
{
  "brandId": 1,
  "answers": {
    "industry": "반려동물 서비스",
    "businessDescription": "검증된 파트너가 반려견 산책을 대신하는 매칭 서비스",
    "targetCustomer": "산책 시간이 부족한 20~40대 반려인",
    "customerProblem": "바쁜 일정 때문에 반려견에게 충분한 산책을 제공하기 어렵다",
    "coreValues": [
      "신뢰",
      "안전",
      "따뜻함"
    ],
    "differentiation": "신원과 경력을 검증한 산책 파트너만 활동한다",
    "desiredImage": "따뜻하지만 전문적이고 믿을 수 있는 이미지"
  },
  "result": {
    "summary": "신뢰와 안전을 중심으로 한 반려동물 서비스",
    "keywords": [
      "신뢰",
      "안전",
      "따뜻함"
    ]
  },
  "createdAt": "2026-07-14T15:30:00+09:00"
}
```

#### 비즈니스 규칙

- `answers`에는 사용자가 브랜드 생성 시 제출한 원본 값을 반환합니다.
- `result`에는 Fake 진단 처리기가 만든 요약과 키워드를 반환합니다.
- 진단은 브랜드의 필수 구성 요소이므로 `NAMING`부터 `FINAL`까지 현재 단계와 관계없이 조회할 수 있습니다.
- 진단 수정 API는 현재 제공하지 않습니다.
- 인증 사용자 ID는 요청으로 받지 않고 Access Token에서 확인합니다.

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `401` | `AUTHENTICATION_REQUIRED` | Access Token 없음 |
| `401` | `INVALID_TOKEN` | Access Token 검증 실패 |
| `401` | `LOGIN_SESSION_REVOKED` | 현재 활성 로그인이 아님 |
| `403` | `BRAND_ACCESS_DENIED` | 브랜드 소유자가 아님 |
| `404` | `BRAND_NOT_FOUND` | 브랜드가 존재하지 않음 |
| `404` | `DIAGNOSIS_NOT_FOUND` | 브랜드의 진단 데이터가 존재하지 않음 |

### 8.7 네이밍 후보 생성

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `POST` |
| Endpoint | `/api/v1/brands/{brandId}/naming/candidates` |
| 인증 | Access Token 필요 |
| 권한 | 브랜드 소유자 |
| 설명 | 진단 결과와 네이밍 추가 답변을 기반으로 후보 3개를 생성합니다. |
| 멱등성 | 멱등하지 않음. 재호출 시 새로운 후보 세트가 생성됩니다. |

#### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `brandId` | Long | 예 | 네이밍을 진행할 브랜드 ID |

#### Request Body

```json
{
  "preferredLanguage": "KOREAN",
  "nameStyle": "짧고 기억하기 쉬운 추상적인 이름",
  "preferredKeywords": [
    "함께",
    "산책"
  ],
  "excludedKeywords": [
    "펫",
    "독"
  ]
}
```

#### 고정 DTO 필드

| 필드 | 타입 | 필수 | 정책 |
| --- | --- | --- | --- |
| `preferredLanguage` | Enum | 예 | `KOREAN`, `ENGLISH`, `MIXED` |
| `nameStyle` | String | 예 | 공백 제거, 5~300자 |
| `preferredKeywords` | String Array | 아니오 | 최대 5개, 항목당 1~30자, 중복 불가 |
| `excludedKeywords` | String Array | 아니오 | 최대 5개, 항목당 1~30자, 중복 불가 |

#### Success Response

```http
HTTP/1.1 201 Created
Content-Type: application/json
```

```json
{
  "brandId": 1,
  "generationId": 2,
  "generationNumber": 1,
  "candidates": [
    {
      "candidateId": 101,
      "name": "동행",
      "rationale": "반려인과 산책 파트너가 함께한다는 의미입니다."
    },
    {
      "candidateId": 102,
      "name": "포우크",
      "rationale": "발걸음과 산책을 연상시키는 조어입니다."
    },
    {
      "candidateId": 103,
      "name": "같이가",
      "rationale": "함께 걷는 서비스 경험을 직관적으로 표현합니다."
    }
  ],
  "generatedAt": "2026-07-14T15:40:00+09:00"
}
```

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `400` | `VALIDATION_FAILED` | 네이밍 답변 검증 실패 |
| `401` | `AUTHENTICATION_REQUIRED` | Access Token 없음 |
| `403` | `BRAND_ACCESS_DENIED` | 브랜드 소유자가 아님 |
| `404` | `BRAND_NOT_FOUND` | 브랜드가 존재하지 않음 |
| `409` | `BRAND_STEP_CONFLICT` | 현재 단계가 `NAMING`이 아님 |
| `409` | `NAMING_ALREADY_SELECTED` | 네이밍 선택이 이미 완료됨 |
| `409` | `CANDIDATE_GENERATION_LIMIT_EXCEEDED` | 최대 생성 횟수 3회 초과 |
| `500` | `CANDIDATE_GENERATION_FAILED` | 후보 생성 중 예상하지 못한 오류 |

#### 비즈니스 규칙

- 브랜드 소유자만 후보를 생성할 수 있습니다.
- 브랜드의 현재 단계가 `NAMING`이어야 합니다.
- 한 번의 생성 요청은 정확히 3개의 후보를 반환합니다.
- 각 후보에는 서버가 발급한 `candidateId`가 존재합니다.
- 최초 생성을 포함해 최대 3회까지 후보를 생성할 수 있습니다.
- 사용자가 네이밍을 선택하기 전까지만 재생성할 수 있습니다.
- 새 후보 세트가 생성되면 이전 Generation을 `SUPERSEDED`로 변경하고 연결된 후보 데이터는 삭제합니다.
- 가장 최근에 생성된 활성 후보 세트의 후보만 선택할 수 있습니다.
- 최대 생성 횟수를 초과한 요청은 `409 CANDIDATE_GENERATION_LIMIT_EXCEEDED`로 거부합니다.

### 8.8 네이밍 후보 선택

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `PUT` |
| Endpoint | `/api/v1/brands/{brandId}/naming/selection` |
| 인증 | Access Token 필요 |
| 권한 | 브랜드 소유자 |
| 설명 | 서버가 생성한 네이밍 후보 하나를 최종 선택합니다. |
| 멱등성 | 같은 후보의 반복 선택은 같은 결과를 유지하도록 설계합니다. |

#### Request Body

```json
{
  "candidateId": 101
}
```

선택한 이름 문자열을 직접 받지 않고 서버가 발급한 `candidateId`만 받습니다.

#### Success Response

```http
HTTP/1.1 200 OK
```

```json
{
  "brandId": 1,
  "selectedNaming": {
    "candidateId": 101,
    "name": "동행",
    "rationale": "반려인과 산책 파트너가 함께한다는 의미입니다."
  },
  "currentStep": "CONCEPT",
  "selectedAt": "2026-07-14T15:45:00+09:00"
}
```

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `400` | `VALIDATION_FAILED` | `candidateId` 누락 또는 형식 오류 |
| `403` | `BRAND_ACCESS_DENIED` | 브랜드 소유자가 아님 |
| `404` | `BRAND_NOT_FOUND` | 브랜드가 존재하지 않음 |
| `404` | `CANDIDATE_NOT_FOUND` | 후보가 존재하지 않거나 해당 브랜드의 후보가 아님 |
| `409` | `BRAND_STEP_CONFLICT` | 현재 단계가 `NAMING`이 아님 |
| `404` | `CANDIDATE_NOT_FOUND` | 후보가 삭제됐거나 현재 선택 가능한 후보가 아님 |

#### 비즈니스 규칙

- 서버가 해당 브랜드를 위해 생성한 후보만 선택할 수 있습니다.
- 현재 활성 후보 세트에 포함된 후보만 선택할 수 있습니다.
- 선택 성공 시 네이밍 결과를 저장하고 `currentStep`을 `CONCEPT`로 변경합니다.
- 같은 후보에 대한 재시도는 기존 선택 결과를 반환할 수 있게 구현합니다.
- 네이밍 완료 후 다른 후보로 변경하는 기능은 현재 제공하지 않습니다.

#### 설계 메모

- 선택 결과는 브랜드 단계별 하나인 하위 리소스이므로 `PUT /naming/selection`을 사용합니다.
- 문자열을 직접 저장하는 기존 방식보다 후보 소유 관계와 조작 방지를 명확하게 검증할 수 있습니다.

### 8.9 콘셉트 후보 생성

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `POST` |
| Endpoint | `/api/v1/brands/{brandId}/concept/candidates` |
| 인증 | Access Token 필요 |
| 권한 | 브랜드 소유자 |
| 설명 | 저장된 진단 결과, 선택된 네이밍과 콘셉트 추가 답변을 기반으로 후보 3개를 생성합니다. |
| 멱등성 | 멱등하지 않음. 재호출 시 새로운 후보 세트가 생성됩니다. |

#### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `brandId` | Long | 예 | 콘셉트를 진행할 브랜드 ID |

#### Request Body

```json
{
  "coreValues": [
    "TRUST",
    "CUSTOMER_FOCUS"
  ],
  "brandVoice": "FRIENDLY_GUIDE",
  "brandPromise": "반려인에게 가장 편안한 산책 경험을 제공합니다.",
  "keyMessage": "함께라서 더 즐거운 산책",
  "conceptVibe": "따뜻하고 친근하지만 깔끔한 분위기",
  "positioningDirections": [
    "MASS_FRIENDLY",
    "SIMPLE_INTUITIVE"
  ]
}
```

#### 고정 DTO 필드

| 필드 | 타입 | 필수 | 정책 |
| --- | --- | --- | --- |
| `coreValues` | Enum Array | 예 | 2~3개, 중복 불가 |
| `brandVoice` | Enum | 예 | 브랜드가 고객에게 말하는 방식 1개 |
| `brandPromise` | String | 예 | 공백 제거, 5~300자 |
| `keyMessage` | String | 예 | 공백 제거, 2~100자 |
| `conceptVibe` | String | 예 | 공백 제거, 2~200자 |
| `positioningDirections` | Enum Array | 예 | 1~2개, 중복 불가 |

초기 Enum 후보는 다음과 같습니다.

- `coreValues`: `INNOVATION`, `TRUST`, `SIMPLICITY`, `SPEED`, `CUSTOMER_FOCUS`, `QUALITY`, `COLLABORATION`, `SUSTAINABILITY`, `ACCESSIBILITY`
- `brandVoice`: `PROFESSIONAL_EXPERT`, `FRIENDLY_GUIDE`, `WITTY_FRIEND`, `SUPPORTIVE_COACH`, `MINIMALIST`
- `positioningDirections`: `MASS_FRIENDLY`, `PREMIUM_LUXURY`, `FAST_EFFICIENT`, `INNOVATIVE_EXPERIMENTAL`, `SIMPLE_INTUITIVE`, `FUN_WITTY`, `STABLE_CONSERVATIVE`

`diagnosisSummary`, 선택된 이름, 생성 회차, 사용자 ID는 요청으로 받지 않습니다. 백엔드가 인증 정보와 저장된 브랜드 데이터에서 조회하거나 계산합니다.

#### Success Response

```http
HTTP/1.1 201 Created
Content-Type: application/json
```

```json
{
  "brandId": 1,
  "generationId": 3,
  "generationNumber": 1,
  "candidates": [
    {
      "candidateId": 201,
      "title": "따뜻한 동행",
      "statement": "매일의 산책을 서로를 이해하는 따뜻한 시간으로 만듭니다.",
      "rationale": "친근함과 신뢰를 중심으로 선택된 이름과 고객 경험을 연결합니다.",
      "brandValues": ["신뢰", "고객 중심", "단순함"]
    },
    {
      "candidateId": 202,
      "title": "가벼운 연결",
      "statement": "누구나 부담 없이 시작하는 반려 생활의 연결점입니다.",
      "rationale": "직관성과 대중성을 강조한 콘셉트입니다.",
      "brandValues": ["접근성", "단순함", "협력"]
    },
    {
      "candidateId": 203,
      "title": "함께 걷는 기준",
      "statement": "사람과 반려동물이 함께 걷는 더 나은 방법을 제안합니다.",
      "rationale": "신뢰할 수 있는 가이드 이미지를 강화한 콘셉트입니다.",
      "brandValues": ["신뢰", "품질", "고객 중심"]
    }
  ],
  "generatedAt": "2026-07-16T15:40:00+09:00"
}
```

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `400` | `VALIDATION_FAILED` | 콘셉트 답변 검증 실패 |
| `401` | `AUTHENTICATION_REQUIRED` | Access Token 없음 |
| `403` | `BRAND_ACCESS_DENIED` | 브랜드 소유자가 아님 |
| `404` | `BRAND_NOT_FOUND` | 브랜드가 존재하지 않음 |
| `409` | `BRAND_STEP_CONFLICT` | 현재 단계가 `CONCEPT`가 아님 |
| `409` | `CONCEPT_ALREADY_SELECTED` | 콘셉트 선택이 이미 완료됨 |
| `409` | `CANDIDATE_GENERATION_LIMIT_EXCEEDED` | 최대 생성 횟수 3회 초과 |
| `500` | `CANDIDATE_GENERATION_FAILED` | 후보 생성 중 예상하지 못한 오류 |

#### 비즈니스 규칙

- 브랜드 소유자만 후보를 생성할 수 있습니다.
- 네이밍 선택이 끝나 `currentStep`이 `CONCEPT`인 브랜드만 요청할 수 있습니다.
- 백엔드는 진단 결과와 선택된 네이밍을 DB에서 조회해 생성 입력에 포함합니다.
- 한 번의 요청은 후보 3개를 생성하며 최초 생성을 포함해 최대 3회 허용합니다.
- 새 후보 세트 생성 시 이전 Generation을 `SUPERSEDED`로 변경하고 연결된 후보 데이터는 삭제합니다.
- 콘셉트를 선택한 뒤에는 다시 생성할 수 없습니다.

### 8.10 콘셉트 후보 선택

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `PUT` |
| Endpoint | `/api/v1/brands/{brandId}/concept/selection` |
| 인증 | Access Token 필요 |
| 권한 | 브랜드 소유자 |
| 설명 | 서버가 생성한 콘셉트 후보 하나를 최종 선택합니다. |
| 멱등성 | 같은 후보의 반복 선택은 같은 결과를 유지하도록 설계합니다. |

#### Request Body

```json
{
  "candidateId": 201
}
```

#### Success Response

```http
HTTP/1.1 200 OK
```

```json
{
  "brandId": 1,
  "selectedConcept": {
    "candidateId": 201,
    "title": "따뜻한 동행",
    "statement": "매일의 산책을 서로를 이해하는 따뜻한 시간으로 만듭니다.",
    "rationale": "친근함과 신뢰를 중심으로 선택된 이름과 고객 경험을 연결합니다.",
    "brandValues": ["신뢰", "고객 중심", "단순함"]
  },
  "currentStep": "STORY",
  "selectedAt": "2026-07-16T15:45:00+09:00"
}
```

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `400` | `VALIDATION_FAILED` | `candidateId` 누락 또는 형식 오류 |
| `403` | `BRAND_ACCESS_DENIED` | 브랜드 소유자가 아님 |
| `404` | `BRAND_NOT_FOUND` | 브랜드가 존재하지 않음 |
| `404` | `CANDIDATE_NOT_FOUND` | 후보가 존재하지 않거나 해당 브랜드의 후보가 아님 |
| `409` | `BRAND_STEP_CONFLICT` | 현재 단계가 `CONCEPT`가 아님 |
| `404` | `CANDIDATE_NOT_FOUND` | 후보가 삭제됐거나 현재 선택 가능한 후보가 아님 |

#### 비즈니스 규칙

- 해당 브랜드의 `ACTIVE` Generation에 남아 있는 후보만 선택할 수 있습니다.
- 선택 성공 시 콘셉트 결과를 저장하고 `currentStep`을 `STORY`로 변경합니다.
- 같은 후보에 대한 재시도는 기존 선택 결과를 반환할 수 있게 구현합니다.
- 콘셉트 완료 후 다른 후보로 변경하는 기능은 현재 제공하지 않습니다.

### 8.11 스토리 후보 생성

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `POST` |
| Endpoint | `/api/v1/brands/{brandId}/story/candidates` |
| 인증 | Access Token 필요 |
| 권한 | 브랜드 소유자 |
| 설명 | 저장된 진단·네이밍·콘셉트 결과와 스토리 추가 답변을 기반으로 후보 3개를 생성합니다. |
| 멱등성 | 멱등하지 않음. 재호출 시 새로운 후보 세트가 생성됩니다. |

#### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `brandId` | Long | 예 | 스토리를 진행할 브랜드 ID |

#### Request Body

```json
{
  "originStory": "반려견과 산책할 친구를 구하기 어려웠던 개인적인 경험에서 시작했습니다.",
  "customerTransformation": "혼자 걷던 산책이 신뢰할 수 있는 이웃과 함께하는 시간이 됩니다.",
  "storyStructure": "PROBLEM_SOLUTION",
  "emotionalTones": [
    "RELIEF",
    "BELONGING"
  ]
}
```

#### 고정 DTO 필드

| 필드 | 타입 | 필수 | 정책 |
| --- | --- | --- | --- |
| `originStory` | String | 아니오 | 공백 제거, 입력 시 5~500자 |
| `customerTransformation` | String | 예 | 공백 제거, 5~500자 |
| `storyStructure` | Enum | 예 | 원하는 이야기 전개 방식 1개 |
| `emotionalTones` | Enum Array | 예 | 1~2개, 중복 불가 |

초기 Enum 후보는 다음과 같습니다.

- `storyStructure`: `PROBLEM_SOLUTION`, `VISIONARY`, `HERO_JOURNEY`, `FOUNDING_STORY`
- `emotionalTones`: `RELIEF`, `CURIOSITY`, `EXCITEMENT`, `EMPOWERMENT`, `BELONGING`, `NOSTALGIA`

창업 계기가 아직 없는 예비 창업자도 서비스를 사용할 수 있으므로 `originStory`는 선택값으로 둡니다. 고객 문제, 브랜드 미션, 핵심 메시지와 선택된 이름·콘셉트는 다시 입력받지 않고 백엔드가 이전 단계 데이터에서 가져옵니다.

#### Success Response

```http
HTTP/1.1 201 Created
Content-Type: application/json
```

```json
{
  "brandId": 1,
  "generationId": 4,
  "generationNumber": 1,
  "candidates": [
    {
      "candidateId": 301,
      "title": "혼자였던 산책에서 함께 걷는 일상으로",
      "storyText": "동행은 혼자 걷는 산책에서 시작된 작은 불편을 발견했습니다. 이제 반려인들은 신뢰할 수 있는 이웃을 만나 함께 걷고, 평범했던 산책을 따뜻한 연결의 시간으로 바꿉니다.",
      "rationale": "고객 문제와 해결 이후의 변화를 선명하게 보여주는 문제 해결형 이야기입니다.",
      "emotionalTones": ["안도감", "소속감"]
    },
    {
      "candidateId": 302,
      "title": "모든 발걸음이 연결되는 동네",
      "storyText": "우리는 사람과 반려동물이 외롭지 않게 걷는 동네를 꿈꿉니다. 동행은 한 번의 산책을 이웃과 관계를 만드는 시작으로 바꾸며 더 가까운 지역사회를 만들어갑니다.",
      "rationale": "브랜드가 만들고 싶은 미래를 중심으로 풀어낸 비전형 이야기입니다.",
      "emotionalTones": ["설렘", "소속감"]
    },
    {
      "candidateId": 303,
      "title": "작은 불편에서 시작된 동행",
      "storyText": "함께 산책할 사람을 찾기 어려웠던 한 번의 경험은 새로운 질문이 되었습니다. 동행은 그 질문에 답하며 누구나 안심하고 산책 친구를 만나는 기준을 만들어갑니다.",
      "rationale": "창업 계기와 브랜드의 성장을 연결한 탄생 이야기입니다.",
      "emotionalTones": ["호기심", "안도감"]
    }
  ],
  "generatedAt": "2026-07-16T16:10:00+09:00"
}
```

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `400` | `VALIDATION_FAILED` | 스토리 답변 검증 실패 |
| `401` | `AUTHENTICATION_REQUIRED` | Access Token 없음 |
| `403` | `BRAND_ACCESS_DENIED` | 브랜드 소유자가 아님 |
| `404` | `BRAND_NOT_FOUND` | 브랜드가 존재하지 않음 |
| `409` | `BRAND_STEP_CONFLICT` | 현재 단계가 `STORY`가 아님 |
| `409` | `STORY_ALREADY_SELECTED` | 스토리 선택이 이미 완료됨 |
| `409` | `CANDIDATE_GENERATION_LIMIT_EXCEEDED` | 최대 생성 횟수 3회 초과 |
| `500` | `CANDIDATE_GENERATION_FAILED` | 후보 생성 중 예상하지 못한 오류 |

#### 비즈니스 규칙

- 콘셉트 선택이 끝나 `currentStep`이 `STORY`인 브랜드만 요청할 수 있습니다.
- 백엔드는 진단 결과, 선택된 네이밍과 콘셉트를 DB에서 조회해 생성 입력에 포함합니다.
- 한 번의 요청은 후보 3개를 생성하며 최초 생성을 포함해 최대 3회 허용합니다.
- 새 후보 세트 생성 시 이전 Generation을 `SUPERSEDED`로 변경하고 연결된 후보 데이터는 삭제합니다.
- 스토리를 선택한 뒤에는 다시 생성할 수 없습니다.

### 8.12 스토리 후보 선택

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `PUT` |
| Endpoint | `/api/v1/brands/{brandId}/story/selection` |
| 인증 | Access Token 필요 |
| 권한 | 브랜드 소유자 |
| 설명 | 서버가 생성한 스토리 후보 하나를 최종 선택합니다. |
| 멱등성 | 같은 후보의 반복 선택은 같은 결과를 유지하도록 설계합니다. |

#### Request Body

```json
{
  "candidateId": 301
}
```

#### Success Response

```http
HTTP/1.1 200 OK
```

```json
{
  "brandId": 1,
  "selectedStory": {
    "candidateId": 301,
    "title": "혼자였던 산책에서 함께 걷는 일상으로",
    "storyText": "동행은 혼자 걷는 산책에서 시작된 작은 불편을 발견했습니다. 이제 반려인들은 신뢰할 수 있는 이웃을 만나 함께 걷고, 평범했던 산책을 따뜻한 연결의 시간으로 바꿉니다.",
    "rationale": "고객 문제와 해결 이후의 변화를 선명하게 보여주는 문제 해결형 이야기입니다.",
    "emotionalTones": ["안도감", "소속감"]
  },
  "currentStep": "LOGO",
  "selectedAt": "2026-07-16T16:15:00+09:00"
}
```

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `400` | `VALIDATION_FAILED` | `candidateId` 누락 또는 형식 오류 |
| `403` | `BRAND_ACCESS_DENIED` | 브랜드 소유자가 아님 |
| `404` | `BRAND_NOT_FOUND` | 브랜드가 존재하지 않음 |
| `404` | `CANDIDATE_NOT_FOUND` | 후보가 존재하지 않거나 해당 브랜드의 후보가 아님 |
| `409` | `BRAND_STEP_CONFLICT` | 현재 단계가 `STORY`가 아님 |
| `404` | `CANDIDATE_NOT_FOUND` | 후보가 삭제됐거나 현재 선택 가능한 후보가 아님 |

#### 비즈니스 규칙

- 해당 브랜드의 `ACTIVE` Generation에 남아 있는 후보만 선택할 수 있습니다.
- 선택 성공 시 스토리 결과를 저장하고 `currentStep`을 `LOGO`로 변경합니다.
- 같은 후보에 대한 재시도는 기존 선택 결과를 반환할 수 있게 구현합니다.
- 스토리 완료 후 다른 후보로 변경하는 기능은 현재 제공하지 않습니다.

### 8.13 로고 후보 생성

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `POST` |
| Endpoint | `/api/v1/brands/{brandId}/logo/candidates` |
| 인증 | Access Token 필요 |
| 권한 | 브랜드 소유자 |
| 설명 | 저장된 이전 단계 결과와 로고 선호 답변을 기반으로 Fake 로고 후보 3개를 만들고 로컬에 저장합니다. |
| 멱등성 | 멱등하지 않음. 재호출 시 새로운 후보 세트와 파일이 생성됩니다. |

#### Request Body

```json
{
  "logoType": "COMBINATION",
  "visualMotif": "ABSTRACT_CONCEPT",
  "primaryColor": "#3B7CF3",
  "secondaryColor": "#0A2540",
  "designStyle": "FLAT_MINIMAL",
  "usageChannels": [
    "MOBILE_APP",
    "WEB"
  ],
  "typographyStyle": "ROUNDED_SANS_SERIF"
}
```

#### 고정 DTO 필드

| 필드 | 타입 | 필수 | 정책 |
| --- | --- | --- | --- |
| `logoType` | Enum | 예 | 로고 구성 형태 1개 |
| `visualMotif` | Enum | 예 | 시각적 모티프 1개 |
| `primaryColor` | String | 예 | `#RRGGBB` 형식 |
| `secondaryColor` | String | 아니오 | 입력 시 `#RRGGBB`, 기본 색상과 달라야 함 |
| `designStyle` | Enum | 예 | 디자인 표현 방식 1개 |
| `usageChannels` | Enum Array | 예 | 주요 사용처 1~2개, 중복 불가 |
| `typographyStyle` | Enum | 예 | 글자 표현 방식 1개 |

초기 Enum 후보는 다음과 같습니다.

- `logoType`: `SYMBOL`, `WORDMARK`, `COMBINATION`, `EMBLEM`
- `visualMotif`: `CONCRETE_OBJECT`, `ABSTRACT_CONCEPT`, `GEOMETRIC_SHAPE`, `LETTER_BASED`, `AI_RECOMMENDED`
- `designStyle`: `FLAT_MINIMAL`, `GRADIENT_3D`, `HANDMADE_ILLUSTRATED`, `GEOMETRIC_TECH`, `VINTAGE_RETRO`, `PLAYFUL_WITTY`
- `usageChannels`: `MOBILE_APP`, `WEB`, `SOCIAL_MEDIA`, `PRINT`, `SIGNAGE`, `PACKAGE`
- `typographyStyle`: `SERIF`, `SANS_SERIF`, `ROUNDED_SANS_SERIF`, `HANDWRITTEN`, `DISPLAY`

선택된 이름·콘셉트·스토리와 진단 결과는 요청에 포함하지 않고 백엔드가 DB에서 조회합니다.

#### Success Response

```http
HTTP/1.1 201 Created
Content-Type: application/json
```

```json
{
  "brandId": 1,
  "generationId": 5,
  "generationNumber": 1,
  "candidates": [
    {
      "candidateId": 401,
      "imageUrl": "/api/v1/brands/1/logo/candidates/401/image",
      "concept": "두 개의 발걸음이 하나로 이어지는 심볼과 워드마크의 조합",
      "rationale": "동행과 연결이라는 브랜드 핵심을 간결하게 표현합니다."
    },
    {
      "candidateId": 402,
      "imageUrl": "/api/v1/brands/1/logo/candidates/402/image",
      "concept": "둥근 선으로 표현한 산책 경로와 부드러운 워드마크",
      "rationale": "친근한 브랜드 보이스와 모바일 환경의 가독성을 반영합니다."
    },
    {
      "candidateId": 403,
      "imageUrl": "/api/v1/brands/1/logo/candidates/403/image",
      "concept": "이니셜을 결합한 단순한 연결 심볼",
      "rationale": "작은 크기와 단색 환경에서도 사용할 수 있는 방향입니다."
    }
  ],
  "generatedAt": "2026-07-16T16:40:00+09:00"
}
```

#### Fake 생성과 로컬 저장 규칙

- 실제 이미지 AI 대신 `LogoCandidateGenerator` 인터페이스의 Fake 구현을 사용합니다.
- Fake 구현은 테스트용 샘플 PNG 3개와 고정된 설명을 반환합니다.
- 애플리케이션은 `FileStorage` 인터페이스를 통해 각 이미지를 로컬 디렉터리에 저장합니다.
- 현재 범위에서는 별도 파일 리소스를 만들지 않고 로고 후보에 저장 경로, Content-Type과 크기를 함께 기록합니다.
- 새 후보 세트 생성 시 이전 Generation을 `SUPERSEDED`로 변경하고 후보 데이터와 로컬 이미지 파일을 삭제합니다.
- 브랜드를 삭제할 때 연결된 로고 파일도 함께 삭제합니다.

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `400` | `VALIDATION_FAILED` | 로고 선호 답변 검증 실패 |
| `401` | `AUTHENTICATION_REQUIRED` | Access Token 없음 |
| `403` | `BRAND_ACCESS_DENIED` | 브랜드 소유자가 아님 |
| `404` | `BRAND_NOT_FOUND` | 브랜드가 존재하지 않음 |
| `409` | `BRAND_STEP_CONFLICT` | 현재 단계가 `LOGO`가 아님 |
| `409` | `LOGO_ALREADY_SELECTED` | 로고 선택이 이미 완료됨 |
| `409` | `CANDIDATE_GENERATION_LIMIT_EXCEEDED` | 최대 생성 횟수 3회 초과 |
| `500` | `CANDIDATE_GENERATION_FAILED` | Fake 후보 생성 중 예상하지 못한 오류 |
| `500` | `FILE_STORAGE_FAILED` | 후보 이미지 로컬 저장 실패 |

### 8.14 로고 후보 선택과 브랜드 완료

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `PUT` |
| Endpoint | `/api/v1/brands/{brandId}/logo/selection` |
| 인증 | Access Token 필요 |
| 권한 | 브랜드 소유자 |
| 설명 | 서버가 생성한 로고 후보 하나를 선택하고 브랜드 컨설팅을 완료합니다. |
| 멱등성 | 같은 후보의 반복 선택은 같은 완료 결과를 유지하도록 설계합니다. |

#### Request Body

```json
{
  "candidateId": 401
}
```

외부 이미지 URL이나 파일 경로를 직접 받지 않습니다.

#### Success Response

```http
HTTP/1.1 200 OK
```

```json
{
  "brandId": 1,
  "selectedLogo": {
    "candidateId": 401,
    "imageUrl": "/api/v1/brands/1/logo/candidates/401/image",
    "concept": "두 개의 발걸음이 하나로 이어지는 심볼과 워드마크의 조합",
    "rationale": "동행과 연결이라는 브랜드 핵심을 간결하게 표현합니다."
  },
  "currentStep": "FINAL",
  "status": "COMPLETED",
  "completedAt": "2026-07-16T16:45:00+09:00"
}
```

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `400` | `VALIDATION_FAILED` | `candidateId` 누락 또는 형식 오류 |
| `403` | `BRAND_ACCESS_DENIED` | 브랜드 소유자가 아님 |
| `404` | `BRAND_NOT_FOUND` | 브랜드가 존재하지 않음 |
| `404` | `CANDIDATE_NOT_FOUND` | 후보가 존재하지 않거나 해당 브랜드의 후보가 아님 |
| `409` | `BRAND_STEP_CONFLICT` | 현재 단계가 `LOGO`가 아님 |
| `404` | `CANDIDATE_NOT_FOUND` | 후보가 삭제됐거나 현재 선택 가능한 후보가 아님 |

#### 비즈니스 규칙

- 해당 브랜드의 `ACTIVE` Generation에 남아 있는 후보만 선택할 수 있습니다.
- 선택 시 이미 로컬에 저장된 후보 파일을 최종 로고로 연결하며 다시 다운로드하거나 복사하지 않습니다.
- 선택 성공 시 `currentStep`을 `FINAL`, `status`를 `COMPLETED`로 변경하고 완료 시간을 기록합니다.
- 완료 후 로고 변경 기능은 현재 제공하지 않습니다.

### 8.15 로고 이미지 파일 조회

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `GET` |
| Endpoint | `/api/v1/brands/{brandId}/logo/candidates/{candidateId}/image` |
| 인증 | Access Token 필요 |
| 권한 | 브랜드 소유자 |
| 설명 | 지정한 로고 후보에 연결된 로컬 이미지 바이트를 반환합니다. |
| 멱등성 | 멱등 |

후보 ID만으로 파일을 찾으며 별도의 공개 `fileId`를 사용하지 않습니다.

#### Success Response

```http
HTTP/1.1 200 OK
Content-Type: image/png
Content-Length: 24518
Cache-Control: private, max-age=3600
```

응답 본문은 JSON이 아니라 이미지 바이너리입니다.

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `403` | `BRAND_ACCESS_DENIED` | 브랜드 소유자가 아님 |
| `404` | `BRAND_NOT_FOUND` | 브랜드가 존재하지 않음 |
| `404` | `CANDIDATE_NOT_FOUND` | 로고 후보가 존재하지 않거나 해당 브랜드의 후보가 아님 |
| `500` | `FILE_READ_FAILED` | 로컬 파일 읽기 실패 |

### 8.16 내 브랜드 목록 조회

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `GET` |
| Endpoint | `/api/v1/brands` |
| 인증 | Access Token 필요 |
| 권한 | 인증 사용자 본인 |
| 설명 | 인증 사용자가 소유한 브랜드를 최근 수정 순으로 조회합니다. |
| 멱등성 | 멱등 |

#### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 정책 |
| --- | --- | --- | --- | --- |
| `page` | Integer | 아니오 | `0` | 0 이상, 0부터 시작 |
| `size` | Integer | 아니오 | `20` | 1~100 |
| `status` | Enum | 아니오 | 전체 | `IN_PROGRESS`, `COMPLETED` |

정렬은 `updatedAt DESC`로 고정합니다. 현재 필요한 정렬이 하나뿐이므로 임의의 정렬 필드를 공개하지 않습니다.

사용자 ID는 Query Parameter로 받지 않습니다. Access Token의 인증 사용자 ID를 사용합니다.

#### Success Response

```http
HTTP/1.1 200 OK
```

```json
{
  "items": [
    {
      "brandId": 2,
      "selectedName": "동행",
      "industry": "반려동물 서비스",
      "currentStep": "FINAL",
      "status": "COMPLETED",
      "createdAt": "2026-07-15T10:00:00+09:00",
      "updatedAt": "2026-07-16T16:45:00+09:00",
      "completedAt": "2026-07-16T16:45:00+09:00"
    },
    {
      "brandId": 1,
      "selectedName": null,
      "industry": "교육 서비스",
      "currentStep": "NAMING",
      "status": "IN_PROGRESS",
      "createdAt": "2026-07-14T15:30:00+09:00",
      "updatedAt": "2026-07-14T15:30:00+09:00",
      "completedAt": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1,
  "hasNext": false
}
```

네이밍을 아직 선택하지 않은 브랜드의 `selectedName`은 `null`입니다. 서버가 임의의 작업명을 만들지 않습니다.

브랜드가 하나도 없어도 오류가 아니라 빈 목록을 반환합니다.

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "hasNext": false
}
```

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `400` | `VALIDATION_FAILED` | 페이지 크기 또는 상태 필터가 올바르지 않음 |
| `401` | `AUTHENTICATION_REQUIRED` | Access Token 없음 |
| `401` | `INVALID_TOKEN` | Access Token 검증 실패 |
| `401` | `LOGIN_SESSION_REVOKED` | 현재 활성 로그인이 아님 |

### 8.17 브랜드 상세와 진행 상태 조회

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `GET` |
| Endpoint | `/api/v1/brands/{brandId}` |
| 인증 | Access Token 필요 |
| 권한 | 브랜드 소유자 |
| 설명 | 브랜드 기본 정보, 현재 단계와 단계별 진행 상태를 조회합니다. |
| 멱등성 | 멱등 |

#### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `brandId` | Long | 예 | 조회할 브랜드 ID |

#### Success Response

진행 중인 브랜드의 예시입니다.

```http
HTTP/1.1 200 OK
```

```json
{
  "brandId": 1,
  "industry": "반려동물 서비스",
  "status": "IN_PROGRESS",
  "currentStep": "CONCEPT",
  "steps": {
    "diagnosis": {
      "status": "COMPLETED"
    },
    "naming": {
      "status": "COMPLETED",
      "selectedName": "동행"
    },
    "concept": {
      "status": "IN_PROGRESS",
      "generationCount": 1,
      "hasActiveCandidates": true
    },
    "story": {
      "status": "LOCKED"
    },
    "logo": {
      "status": "LOCKED"
    }
  },
  "createdAt": "2026-07-14T15:30:00+09:00",
  "updatedAt": "2026-07-16T15:40:00+09:00",
  "completedAt": null
}
```

완료된 브랜드에서는 모든 단계가 `COMPLETED`이고 `currentStep`은 `FINAL`입니다. 이 API는 진행 상태 확인용이므로 선택된 콘셉트·스토리의 긴 본문과 후보 목록 전체는 반환하지 않습니다. 완성 결과 전체는 별도의 결과 조회 API에서 제공합니다.

#### 단계 상태 규칙

| 상태 | 의미 |
| --- | --- |
| `COMPLETED` | 후보 선택까지 끝난 단계 |
| `IN_PROGRESS` | 현재 사용자가 진행할 수 있는 단계 |
| `LOCKED` | 이전 단계가 끝나지 않아 접근할 수 없는 단계 |

- `steps`는 DB에 별도 상태값으로 중복 저장하지 않고 `currentStep`과 단계별 선택 결과를 기반으로 계산합니다.
- 현재 단계의 `generationCount`는 지금까지 생성한 후보 세트 수입니다.
- `hasActiveCandidates`가 `true`면 사용자가 후보를 다시 생성하지 않고 기존 활성 후보를 조회해 선택할 수 있습니다.
- 완료된 단계에는 화면 표시에 필요한 짧은 선택 결과만 포함할 수 있습니다.

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `401` | `AUTHENTICATION_REQUIRED` | Access Token 없음 |
| `401` | `INVALID_TOKEN` | Access Token 검증 실패 |
| `401` | `LOGIN_SESSION_REVOKED` | 현재 활성 로그인이 아님 |
| `403` | `BRAND_ACCESS_DENIED` | 브랜드 소유자가 아님 |
| `404` | `BRAND_NOT_FOUND` | 브랜드가 존재하지 않음 |

#### 설계 메모

- 목록 API는 여러 브랜드의 최소 정보만 반환하고, 상세 API는 브랜드 하나의 단계별 진행 상태를 반환합니다.
- 후보 목록은 응답 크기와 DTO 복잡도를 줄이기 위해 포함하지 않습니다.
- 상세 API의 `hasActiveCandidates`가 `true`이면 해당 단계 조회 API로 활성 후보를 가져옵니다.
- 최종 결과 보고서는 진행 상태 조회와 목적이 다르므로 별도 API로 분리합니다.

### 8.18 단계별 최신 후보와 선택 결과 조회

#### Endpoint 목록

| 단계 | Method | Endpoint | Response DTO |
| --- | --- | --- | --- |
| 네이밍 | `GET` | `/api/v1/brands/{brandId}/naming` | `NamingStageResponse` |
| 콘셉트 | `GET` | `/api/v1/brands/{brandId}/concept` | `ConceptStageResponse` |
| 스토리 | `GET` | `/api/v1/brands/{brandId}/story` | `StoryStageResponse` |
| 로고 | `GET` | `/api/v1/brands/{brandId}/logo` | `LogoStageResponse` |

- 인증: Access Token 필요
- 권한: 브랜드 소유자
- 멱등성: 멱등
- 목적: 현재 단계 재개 또는 완료된 단계의 선택 결과 확인

#### 네이밍 조회 응답 예시

후보를 생성했지만 아직 선택하지 않은 경우입니다.

```http
GET /api/v1/brands/1/naming
```

```json
{
  "brandId": 1,
  "stage": "NAMING",
  "stageStatus": "IN_PROGRESS",
  "generationCount": 2,
  "remainingGenerations": 1,
  "canGenerate": true,
  "canSelect": true,
  "latestGeneration": {
    "generationId": 2,
    "generationNumber": 2,
    "generatedAt": "2026-07-16T15:40:00+09:00",
    "candidates": [
      {
        "candidateId": 101,
        "name": "동행",
        "rationale": "함께 걷는 브랜드 경험을 표현합니다."
      },
      {
        "candidateId": 102,
        "name": "포우크",
        "rationale": "발걸음과 산책을 연상시키는 조어입니다."
      },
      {
        "candidateId": 103,
        "name": "같이가",
        "rationale": "함께 걷는 경험을 직관적으로 표현합니다."
      }
    ]
  },
  "selectedCandidate": null
}
```

후보를 아직 한 번도 생성하지 않았다면 오류 대신 다음 값을 반환합니다.

```json
{
  "brandId": 1,
  "stage": "NAMING",
  "stageStatus": "IN_PROGRESS",
  "generationCount": 0,
  "remainingGenerations": 3,
  "canGenerate": true,
  "canSelect": false,
  "latestGeneration": null,
  "selectedCandidate": null
}
```

선택이 끝난 단계에서는 `stageStatus`가 `COMPLETED`이고 `selectedCandidate`에 선택 결과가 들어갑니다. `canGenerate`와 `canSelect`는 모두 `false`입니다.

#### 단계별 후보 DTO

공통 Envelope 구조는 같지만 `candidates`와 `selectedCandidate`의 타입은 단계마다 다릅니다.

| 단계 | 후보 필드 |
| --- | --- |
| 네이밍 | `candidateId`, `name`, `rationale` |
| 콘셉트 | `candidateId`, `title`, `statement`, `rationale`, `brandValues` |
| 스토리 | `candidateId`, `title`, `storyText`, `rationale`, `emotionalTones` |
| 로고 | `candidateId`, `imageUrl`, `concept`, `rationale` |

로고의 `imageUrl`은 다음 형식을 사용합니다.

```text
/api/v1/brands/{brandId}/logo/candidates/{candidateId}/image
```

#### 조회 범위 규칙

- `ACTIVE` Generation의 최신 후보 또는 `SELECTED` Generation의 최종 선택 후보만 반환합니다.
- 현재 진행 중인 단계와 이미 완료된 단계는 조회할 수 있습니다.
- 아직 잠겨 있는 미래 단계 조회는 `409 BRAND_STEP_CONFLICT`로 거부합니다.
- 현재 단계에서 후보가 없다는 것은 정상 상태이므로 `200 OK`와 `latestGeneration: null`을 반환합니다.
- 후보 생성 가능 여부와 선택 가능 여부는 서버가 현재 단계, 생성 횟수와 선택 결과를 기반으로 계산합니다.

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `401` | `AUTHENTICATION_REQUIRED` | Access Token 없음 |
| `401` | `INVALID_TOKEN` | Access Token 검증 실패 |
| `401` | `LOGIN_SESSION_REVOKED` | 현재 활성 로그인이 아님 |
| `403` | `BRAND_ACCESS_DENIED` | 브랜드 소유자가 아님 |
| `404` | `BRAND_NOT_FOUND` | 브랜드가 존재하지 않음 |
| `409` | `BRAND_STEP_CONFLICT` | 아직 접근할 수 없는 미래 단계 조회 |

#### 설계 메모

- `/stages/{stage}` 하나로 합치면 후보 응답이 다시 `Map<String, Object>` 또는 복잡한 다형 DTO가 되므로 단계별 URI를 유지합니다.
- 생성 이력 전체 조회는 현재 사용자 흐름에 필요하지 않으므로 API에서 제외합니다. DB에는 생성 회차 메타데이터만 남기고 `SUPERSEDED` 후보 내용은 삭제합니다.

### 8.19 최종 브랜드 결과 조회

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `GET` |
| Endpoint | `/api/v1/brands/{brandId}/result` |
| 인증 | Access Token 필요 |
| 권한 | 브랜드 소유자 |
| 설명 | 완료된 브랜드의 진단과 최종 선택 결과를 하나의 보고서 형태로 조회합니다. |
| 멱등성 | 멱등 |

#### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `brandId` | Long | 예 | 결과를 조회할 브랜드 ID |

#### Success Response

```http
HTTP/1.1 200 OK
Cache-Control: private, no-store
```

```json
{
  "brandId": 1,
  "status": "COMPLETED",
  "diagnosis": {
    "industry": "반려동물 서비스",
    "businessDescription": "반려인이 함께 산책할 이웃을 찾을 수 있는 연결 서비스",
    "targetCustomer": "혼자 반려견을 산책시키는 20~40대 반려인",
    "customerProblem": "안심하고 함께 산책할 사람을 찾기 어렵습니다.",
    "coreValues": [
      "신뢰",
      "연결",
      "즐거움"
    ],
    "differentiation": "지역과 산책 성향을 기반으로 신뢰할 수 있는 이웃을 연결합니다.",
    "desiredImage": "따뜻하고 믿을 수 있는 동네 친구"
  },
  "naming": {
    "candidateId": 101,
    "name": "동행",
    "rationale": "사람과 반려동물이 함께 걷는 경험을 표현합니다."
  },
  "concept": {
    "candidateId": 201,
    "title": "따뜻한 동행",
    "statement": "매일의 산책을 서로를 이해하는 따뜻한 시간으로 만듭니다.",
    "rationale": "친근함과 신뢰를 중심으로 이름과 고객 경험을 연결합니다.",
    "brandValues": [
      "신뢰",
      "고객 중심",
      "단순함"
    ]
  },
  "story": {
    "candidateId": 301,
    "title": "혼자였던 산책에서 함께 걷는 일상으로",
    "storyText": "동행은 혼자 걷는 산책에서 시작된 작은 불편을 발견했습니다. 이제 반려인들은 신뢰할 수 있는 이웃을 만나 함께 걷고, 평범했던 산책을 따뜻한 연결의 시간으로 바꿉니다.",
    "rationale": "고객 문제와 해결 이후의 변화를 보여주는 문제 해결형 이야기입니다.",
    "emotionalTones": [
      "안도감",
      "소속감"
    ]
  },
  "logo": {
    "candidateId": 401,
    "imageUrl": "/api/v1/brands/1/logo/candidates/401/image",
    "concept": "두 개의 발걸음이 하나로 이어지는 심볼과 워드마크의 조합",
    "rationale": "동행과 연결이라는 브랜드 핵심을 간결하게 표현합니다."
  },
  "createdAt": "2026-07-14T15:30:00+09:00",
  "completedAt": "2026-07-16T16:45:00+09:00"
}
```

#### 비즈니스 규칙

- `status`가 `COMPLETED`이고 `currentStep`이 `FINAL`인 브랜드만 조회할 수 있습니다.
- 진단부터 로고까지 필요한 선택 결과가 모두 존재해야 합니다.
- 응답은 최신 후보 목록이 아니라 각 단계에서 선택된 결과 하나씩만 포함합니다.
- API 호출 시 AI를 다시 실행하지 않고 DB에 저장된 결과만 조회합니다.
- 사용자가 입력한 비밀번호, 인증 정보와 내부 저장 경로는 포함하지 않습니다.
- 로고는 실제 디스크 경로가 아니라 권한 검사를 거치는 이미지 조회 URL로 제공합니다.

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `401` | `AUTHENTICATION_REQUIRED` | Access Token 없음 |
| `401` | `INVALID_TOKEN` | Access Token 검증 실패 |
| `401` | `LOGIN_SESSION_REVOKED` | 현재 활성 로그인이 아님 |
| `403` | `BRAND_ACCESS_DENIED` | 브랜드 소유자가 아님 |
| `404` | `BRAND_NOT_FOUND` | 브랜드가 존재하지 않음 |
| `409` | `BRAND_NOT_COMPLETED` | 아직 최종 완료되지 않은 브랜드 |
| `500` | `BRAND_RESULT_INCONSISTENT` | 완료 상태지만 필수 선택 결과가 누락됨 |

#### 설계 메모

- 상세 API는 진행 재개를 위한 작은 응답이고, 결과 API는 최종 보고서를 위한 큰 응답입니다.
- `BRAND_RESULT_INCONSISTENT`는 정상적인 사용자 입력 오류가 아니라 데이터 무결성 문제이므로 서버 오류로 처리합니다.

### 8.20 브랜드 영구 삭제

#### 기본 정보

| 항목 | 내용 |
| --- | --- |
| Method | `DELETE` |
| Endpoint | `/api/v1/brands/{brandId}` |
| 인증 | Access Token 필요 |
| 권한 | 브랜드 소유자 |
| 설명 | 브랜드와 연결된 모든 데이터 및 로컬 로고 파일을 영구 삭제합니다. |
| 멱등성 | 리소스 상태 관점에서 멱등. 이미 삭제된 브랜드를 다시 요청하면 `404`를 반환합니다. |

#### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `brandId` | Long | 예 | 영구 삭제할 브랜드 ID |

#### Success Response

```http
HTTP/1.1 204 No Content
```

응답 본문은 반환하지 않습니다.

#### 영구 삭제 범위

- Brand
- 기업 진단 답변과 진단 결과
- 네이밍·콘셉트·스토리·로고의 모든 후보 생성 세트
- 현재 후보와 단계별 최종 선택 후보
- 후보 생성 회차 메타데이터
- 단계별 최종 선택 결과
- 로컬 저장소의 로고 후보 이미지 전체

사용자 계정과 사용자의 다른 브랜드는 삭제하지 않습니다.

#### 비즈니스 규칙

- 진행 중인 브랜드와 완료된 브랜드 모두 삭제할 수 있습니다.
- 휴지통, 복구와 삭제 취소 기능은 제공하지 않습니다.
- 삭제할 브랜드의 소유권을 먼저 검증합니다.
- DB 하위 데이터는 외래 키와 삭제 순서를 통해 빠짐없이 제거합니다.
- DB 삭제가 성공한 뒤 브랜드 목록과 상세 조회에서 즉시 나타나지 않아야 합니다.
- 삭제된 `brandId`로 상세·단계·결과·이미지 API를 요청하면 `404`를 반환합니다.

#### DB와 로컬 파일 삭제 일관성

DB 트랜잭션은 로컬 파일 시스템을 롤백할 수 없으므로 다음 보상 절차를 사용합니다.

```text
삭제할 로고 파일을 임시 휴지 디렉터리로 이동
→ DB 트랜잭션에서 브랜드와 하위 데이터 영구 삭제
→ DB Commit 성공: 임시 파일 영구 삭제
→ DB Rollback: 임시 파일을 원래 위치로 복원
```

- 파일을 임시 위치로 이동하지 못하면 DB 삭제를 시작하지 않고 `500 FILE_DELETE_FAILED`를 반환합니다.
- DB Commit 후 임시 파일의 최종 제거가 실패하면 사용자 데이터는 이미 접근 불가능한 상태이므로 실패를 기록하고 재정리 대상으로 남깁니다.
- 구현 단계에서는 애플리케이션 시작 시 오래된 임시 파일을 정리하는 방식을 검토합니다.

#### Error Responses

| Status | Error Code | 발생 조건 |
| ---: | --- | --- |
| `401` | `AUTHENTICATION_REQUIRED` | Access Token 없음 |
| `401` | `INVALID_TOKEN` | Access Token 검증 실패 |
| `401` | `LOGIN_SESSION_REVOKED` | 현재 활성 로그인이 아님 |
| `403` | `BRAND_ACCESS_DENIED` | 브랜드 소유자가 아님 |
| `404` | `BRAND_NOT_FOUND` | 브랜드가 존재하지 않거나 이미 삭제됨 |
| `500` | `FILE_DELETE_FAILED` | 로고 파일을 삭제 준비 상태로 옮기지 못함 |
| `500` | `BRAND_DELETE_FAILED` | DB 데이터 삭제 중 예상하지 못한 오류 |

#### 설계 메모

- 삭제 후 표현할 리소스가 없으므로 `200 OK`와 JSON 대신 `204 No Content`를 사용합니다.
- `DELETE`의 멱등성은 반복 요청의 상태 효과가 같다는 뜻이며 모든 요청의 응답 코드가 반드시 같아야 한다는 뜻은 아닙니다.
- 학습 프로젝트라도 사용자 데이터 삭제 요청이므로 논리 삭제 표시만 남기는 대신 실제 데이터와 파일을 제거합니다.

## 9. 오류 코드 목록

### 공통

| Error Code | Status | 설명 |
| --- | ---: | --- |
| `VALIDATION_FAILED` | 400 | 요청값 검증 실패 |
| `MALFORMED_JSON` | 400 | JSON 형식 오류 |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | 지원하지 않는 Content-Type |
| `INTERNAL_SERVER_ERROR` | 500 | 예상하지 못한 서버 오류 |

### 인증과 사용자

| Error Code | Status | 설명 |
| --- | ---: | --- |
| `DUPLICATE_EMAIL` | 409 | 이미 사용 중인 이메일 |
| `INVALID_CREDENTIALS` | 401 | 이메일이 없거나 비밀번호가 일치하지 않음 |
| `AUTHENTICATION_REQUIRED` | 401 | 인증 정보 없음 |
| `INVALID_TOKEN` | 401 | 변조되거나 올바르지 않은 토큰 |
| `EXPIRED_TOKEN` | 401 | 만료된 토큰 |
| `REFRESH_TOKEN_REQUIRED` | 401 | Refresh Token Cookie 없음 |
| `INVALID_REFRESH_TOKEN` | 401 | Refresh Token 검증 실패 |
| `EXPIRED_REFRESH_TOKEN` | 401 | Refresh Token 만료 |
| `REVOKED_REFRESH_TOKEN` | 401 | 폐기된 Refresh Token |
| `REUSED_REFRESH_TOKEN` | 401 | 회전 후 폐기된 Refresh Token 재사용 |
| `LOGIN_SESSION_REVOKED` | 401 | 현재 활성 로그인이 아닌 세션의 Access Token |
| `ACCESS_DENIED` | 403 | 접근 권한 없음 |
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없음 |

### 브랜드

| Error Code | Status | 설명 |
| --- | ---: | --- |
| `BRAND_NOT_FOUND` | 404 | 브랜드를 찾을 수 없음 |
| `BRAND_ACCESS_DENIED` | 403 | 브랜드 소유자가 아님 |
| `BRAND_STEP_CONFLICT` | 409 | 현재 단계와 요청 작업이 일치하지 않음 |
| `CANDIDATES_NOT_FOUND` | 404 | 선택할 후보가 존재하지 않음 |
| `CANDIDATE_NOT_FOUND` | 404 | 지정한 후보가 존재하지 않음 |
| `BRAND_ALREADY_COMPLETED` | 409 | 이미 완료된 브랜드에 허용되지 않은 변경 요청 |
| `BRAND_NOT_COMPLETED` | 409 | 최종 결과 조회가 가능한 완료 상태가 아님 |
| `BRAND_RESULT_INCONSISTENT` | 500 | 완료 브랜드의 필수 선택 결과 누락 |
| `BRAND_DELETE_FAILED` | 500 | 브랜드 DB 데이터 영구 삭제 실패 |
| `CONCURRENT_BRAND_UPDATE` | 409 | 동시에 브랜드가 변경되어 충돌 발생 |
| `DIAGNOSIS_PROCESSING_FAILED` | 500 | 진단 처리 중 예상하지 못한 오류 |
| `DIAGNOSIS_NOT_FOUND` | 404 | 브랜드에 연결된 진단 데이터 없음 |
| `CANDIDATE_GENERATION_FAILED` | 500 | 후보 생성 중 예상하지 못한 오류 |
| `NAMING_ALREADY_SELECTED` | 409 | 네이밍 선택 완료 후 재생성 요청 |
| `CONCEPT_ALREADY_SELECTED` | 409 | 콘셉트 선택 완료 후 재생성 요청 |
| `STORY_ALREADY_SELECTED` | 409 | 스토리 선택 완료 후 재생성 요청 |
| `LOGO_ALREADY_SELECTED` | 409 | 로고 선택 완료 후 재생성 요청 |
| `CANDIDATE_GENERATION_LIMIT_EXCEEDED` | 409 | 단계별 최대 후보 생성 횟수 초과 |

### 파일

| Error Code | Status | 설명 |
| --- | ---: | --- |
| `FILE_STORAGE_FAILED` | 500 | 파일 저장 실패 |
| `FILE_READ_FAILED` | 500 | 로컬 파일 읽기 실패 |
| `FILE_DELETE_FAILED` | 500 | 로컬 파일 삭제 준비 또는 정리 실패 |

## 10. Stage 1 확정 사항

- [x] 핵심 사용자와 서비스 목표 한 문장 확정
- [x] 회원가입의 로그인 식별자를 이메일로 확정
- [x] 회원가입 필드별 상세 검증 규칙 확정
- [x] 인증 방식을 JWT Access Token과 서버 관리 Refresh Token으로 확정
- [x] Access Token 15분, Refresh Token 14일로 만료시간 확정
- [x] Refresh Token을 HttpOnly Cookie로 전달하고 서버에는 해시 저장
- [x] Refresh Token 사용 시 회전하고 로그아웃 시 폐기
- [x] 한 사용자당 하나의 활성 로그인만 허용
- [x] 새 로그인 성공 시 기존 로그인을 폐기하고 새 로그인으로 대체
- [x] Access Token의 `sid`를 활성 Session ID와 비교해 기존 로그인을 즉시 무효화
- [x] Base URL에 `/api/v1` 사용
- [x] 진단 제출 성공 시 브랜드를 생성하도록 결정
- [x] 공개 API 입력은 `Map<String, Object>`가 아닌 고정 Request DTO 사용
- [x] 단계별 후보는 최초 생성을 포함해 최대 3회, 회당 3개 생성
- [x] 선택 전 재생성을 허용하고 최신 후보 세트만 선택 가능
- [x] 선택 완료 후 이전 단계 선택 변경은 현재 제공하지 않음
- [x] 완료된 브랜드는 조회와 영구 삭제만 허용하고 결과 수정은 제공하지 않음
- [x] 단계별 조회 API는 최신 후보와 선택 결과만 제공하고 전체 생성 이력은 제외
- [x] 브랜드 상세는 진행 상태 중심, 완성 결과는 별도 결과 API로 분리
- [x] Fake 로고 이미지를 로컬에 저장하고 파일 조회 API로 제공
- [x] 사용자 로고 파일 업로드는 현재 범위에서 제외
- [x] 브랜드 목록은 0부터 시작하는 페이지 번호와 최대 100개의 페이지네이션 사용
- [x] 브랜드와 하위 데이터 및 로컬 파일을 영구 삭제

## 11. 변경 이력

| 버전 | 날짜 | 변경 내용 |
| --- | --- | --- |
| `0.1.0` | 2026-07-14 | API 명세 기본 구조와 엔드포인트 초안 생성 |
| `0.2.0` | 2026-07-14 | 핵심 사용자·서비스 정의 및 브랜드 생성과 진단 제출 분리 결정 |
| `0.3.0` | 2026-07-14 | 이메일 로그인 결정 및 회원가입 API 상세 초안 작성 |
| `0.4.0` | 2026-07-14 | 회원가입 이메일·비밀번호·이름 검증 정책 확정 |
| `0.5.0` | 2026-07-14 | JWT 인증 결정 및 로그인·토큰 API 상세 초안 작성 |
| `0.6.0` | 2026-07-14 | 토큰 만료·전달·회전 정책 및 재발급·로그아웃 API 확정 |
| `0.7.0` | 2026-07-14 | 한 사용자당 하나의 활성 로그인만 허용하는 정책 확정 |
| `0.8.0` | 2026-07-14 | 기존 로그인 즉시 대체 정책 확정 및 내 정보 조회 API 초안 작성 |
| `0.9.0` | 2026-07-14 | 빈 브랜드 생성을 제거하고 진단 제출 성공 시 브랜드를 생성하도록 변경 |
| `0.10.0` | 2026-07-14 | 진단 답변 Request DTO와 브랜드 생성 API 상세 초안 작성 |
| `0.11.0` | 2026-07-14 | 고정 DTO 사용 확정 및 네이밍 후보 생성·선택 API 초안 작성 |
| `0.12.0` | 2026-07-16 | 후보 재생성 정책 확정 및 콘셉트 후보 생성·선택 API 초안 작성 |
| `0.13.0` | 2026-07-16 | 중복 질문을 줄인 스토리 후보 생성·선택 API 초안 작성 |
| `0.14.0` | 2026-07-16 | Fake 로고 생성·로컬 파일 저장·후보 선택 API 초안 작성 |
| `0.15.0` | 2026-07-16 | 공개 fileId 제거 및 내 브랜드 목록·페이지네이션 API 초안 작성 |
| `0.16.0` | 2026-07-16 | 브랜드 상세·단계별 진행 상태 API 초안 작성 |
| `0.17.0` | 2026-07-16 | 단계별 최신 후보·선택 결과 조회 API 초안 작성 |
| `0.18.0` | 2026-07-16 | 완료된 브랜드 최종 결과 조회 API 초안 작성 |
| `0.19.0` | 2026-07-16 | 브랜드 및 연결 데이터·로컬 파일 영구 삭제 API 확정 |
| `0.20.0` | 2026-07-16 | 진단 원본 답변과 처리 결과 조회 API 확정 |
| `0.21.0` | 2026-07-16 | 단계 순서를 유지하면서 현재 단계부터 중단 후 재개하도록 확정 |
| `0.22.0` | 2026-07-16 | 조회 API가 없는 후보 생성 회차의 Location 헤더 제거 |
| `0.23.0` | 2026-07-16 | API 전체 목록의 상태와 인증 표기를 확정 명세에 맞게 통일 |
| `0.24.0` | 2026-07-16 | 요구사항이 없는 비밀번호 변경 API를 범위에서 제외 |
| `0.25.0` | 2026-07-16 | 회원가입·로그인을 제외한 사용자 부가기능을 범위에서 제외 |
| `1.0.0` | 2026-07-16 | 최종 일관성 점검, 회원가입 Location 제거 및 Stage 1 API 계약 완료 |
| `1.1.0` | 2026-07-16 | 재생성·선택 후 불필요한 후보와 로고 파일을 삭제하는 보관 정책 반영 |
