# SSAIREN Dashboard Backend 구현 요구사항

## 1. 목적과 범위

이 문서는 `DashBoard` 프론트엔드가 실제 데이터를 받아 동작하기 위해 Spring Boot `backend`가 구현해야 할 기능을 정리한다.

- 기준 화면: 실시간 모니터링(`/`), 통계(`/statistics`)
- 외부 연결: Flutter -> Spring Boot -> FastAPI -> Spring Boot -> Dashboard
- 통신 방식:
  - 초기 조회, 통계 조회, 사건 종료: REST API
  - 신규 사건 및 사건 진행 상태 갱신: WebSocket
- STT 처리:
  - Flutter가 피해자 WebSocket 연결로 STT 텍스트 청크를 Spring Boot에 지속 전송
  - Spring Boot가 청크를 순서대로 저장하고 일정량이 누적되면 FastAPI에 분석/요약 요청
  - FastAPI 결과에 따라 사건을 생성하거나 기존 사건을 갱신
- 현재 백엔드는 Spring Boot, JPA, PostgreSQL, Swagger 설정만 존재하며 Controller, Service, Repository, Entity, WebSocket 설정은 아직 없다.

### 이번 범위에서 제외할 기능

프론트엔드 README와 현재 UI에서 실제 동작이 없다고 명시된 항목이다.

- 현장 출동 지시 버튼 동작
- 피해자 연결 버튼 동작
- 로그인 및 사용자 정보 조회
- 알림 목록 조회
- 설정 화면

---

## 2. 프론트엔드가 요구하는 데이터

### 사건(Case)

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | Long | 사건 식별자 |
| `caseNumber` | String | 화면 표시용 사건 번호. 예: `#2026-000001` |
| `name` | String | 마스킹된 피해자 이름 |
| `age` | Integer | 피해자 나이 |
| `types` | String[] | 피해 유형 목록 |
| `risk` | Integer | 위험 점수, 0~100 |
| `status` | String | `IN_PROGRESS`, `COMPLETED` |
| `detectedAt` | ISO-8601 DateTime | 최초 탐지 시각 |
| `elapsedSeconds` | Long | 통화 또는 사건 경과 시간 |
| `location` | String | 화면 표시용 위치명 |
| `lat` | Double | 피해자 위도 |
| `lng` | Double | 피해자 경도 |
| `guardianLat` | Double/null | 납치협박 유형일 때 보호자 위치 |
| `guardianLng` | Double/null | 납치협박 유형일 때 보호자 위치 |
| `keywords` | String[] | 탐지 키워드 |
| `summary` | String[] | AI 통화 분석 요약 문장 목록 |
| `process` | Object | `gps`, `sms`, `police` 단계 상태 |
| `policeStation` | Object/null | 담당 또는 인근 경찰서 정보 |
| `completedAt` | ISO-8601 DateTime/null | 사건 종료 시각 |

### 피해 유형 enum

- `AGENCY_IMPERSONATION`: 기관사칭
- `KIDNAPPING_THREAT`: 납치협박
- `ACCOUNT_TRANSFER_INDUCEMENT`: 계좌이체유도
- `REMOTE_APP_INSTALLATION`: 원격앱설치

API 내부 값은 enum 형태로 고정하고, 화면 표시 문구는 별도 필드 또는 프론트엔드 매핑으로 처리하는 것을 권장한다. 현재 프론트엔드 목업의 `계좌이체요도`는 오타이므로 API 계약에는 사용하지 않는다.

### 대응 프로세스

각 단계는 Boolean보다 상태 enum으로 저장하는 것이 안전하다.

```json
{
  "gps": {
    "status": "COMPLETED",
    "detail": "서울시 강남구",
    "completedAt": "2026-06-10T14:22:11+09:00"
  },
  "sms": {
    "status": "PENDING",
    "detail": null,
    "completedAt": null
  },
  "police": {
    "status": "PENDING",
    "detail": null,
    "completedAt": null
  }
}
```

상태 값: `PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`

---

## 3. 필수 REST API

공통 prefix는 `/api`를 사용한다. 시간 값은 ISO-8601, 서버 기준 시간대는 명시적으로 관리한다.

### 3.1 실시간 모니터링

#### 진행 중 사건 목록 조회

`GET /api/cases?status=IN_PROGRESS&sort=risk,desc`

페이지 새로고침 또는 WebSocket 재연결 시 현재 진행 중 사건을 복구하기 위해 필요하다. WebSocket만 사용하면 연결 전에 발생한 사건을 받을 수 없다.

응답 예시:

```json
{
  "items": [
    {
      "id": 1,
      "caseNumber": "#2026-000001",
      "name": "김OO",
      "age": 71,
      "types": ["AGENCY_IMPERSONATION", "ACCOUNT_TRANSFER_INDUCEMENT"],
      "risk": 92,
      "status": "IN_PROGRESS",
      "detectedAt": "2026-06-10T14:20:00+09:00",
      "elapsedSeconds": 143,
      "location": "서울 강남구",
      "lat": 37.4979,
      "lng": 127.0276,
      "guardianLat": null,
      "guardianLng": null,
      "keywords": ["검찰 수사관", "계좌", "이체"],
      "summary": [
        "검찰 수사관을 사칭하며 접근",
        "피해자 계좌가 범죄에 연루됐다고 주장",
        "안전계좌로 즉시 이체 요구"
      ],
      "process": {
        "gps": { "status": "COMPLETED" },
        "sms": { "status": "PENDING" },
        "police": { "status": "PENDING" }
      },
      "policeStation": {
        "name": "서초경찰서",
        "distanceMeters": 1200,
        "status": "WAITING"
      },
      "completedAt": null
    }
  ]
}
```

#### 사건 상세 조회

`GET /api/cases/{caseId}`

- 목록 응답을 가볍게 유지하고 상세 패널 진입 시 전체 정보를 조회하려면 구현한다.
- 초기 버전에서 목록 API가 모든 상세 필드를 반환한다면 생략 가능하다.

#### 대시보드 상단 요약 조회

`GET /api/dashboard/summary`

응답 예시:

```json
{
  "inProgressCount": 3,
  "completedCount": 18,
  "averageResponseSeconds": 134,
  "calculatedAt": "2026-06-10T14:30:00+09:00"
}
```

`completedCount`의 집계 기간은 반드시 정의해야 한다. 프론트 화면 용도상 기본값은 당일 집계를 권장한다.

#### 사건 종료

`PATCH /api/cases/{caseId}/status`

요청:

```json
{
  "status": "COMPLETED"
}
```

처리 요구사항:

- 이미 종료된 사건에 대한 중복 요청은 멱등 처리한다.
- `completedAt`과 대응 시간을 기록한다.
- 성공 후 `CASE_UPDATED` WebSocket 이벤트를 모든 대시보드에 전송한다.
- 존재하지 않는 사건은 `404`, 허용되지 않은 상태 전이는 `409`로 응답한다.

### 3.2 통계 화면

모든 통계 API는 기본 집계 기간과 선택 가능한 기간을 명시해야 한다. 현재 UI의 전주 대비 증감률을 지원하기 위해 기본 기간은 최근 7일, 비교 기간은 직전 7일을 권장한다.

공통 선택 query:

- `from`: 집계 시작 시각
- `to`: 집계 종료 시각
- 생략 시 최근 7일

#### 통계 요약

`GET /api/statistics/summary`

```json
{
  "totalDetections": { "count": 1284, "changeRate": 12.0 },
  "highRiskDetections": { "count": 342, "changeRate": 8.0 },
  "accountFreezeRequests": { "count": 89, "changeRate": -3.0 },
  "period": {
    "from": "2026-06-04T00:00:00+09:00",
    "to": "2026-06-10T23:59:59+09:00"
  }
}
```

고위험 기준값은 설정으로 관리해야 한다. Dashboard README 기준 트리거 임계값은 `0.8`, 화면 점수 기준으로는 `80`이다.

#### 탐지 키워드 TOP 10

`GET /api/statistics/keywords?limit=10`

```json
{
  "items": [
    { "rank": 1, "word": "검찰청", "count": 425 }
  ]
}
```

#### 연령대별 피해 현황

`GET /api/statistics/age-groups`

```json
{
  "items": [
    { "label": "60대 이상", "count": 578, "ratio": 45.0 }
  ],
  "highestGroup": "60대 이상"
}
```

비율은 문자열이 아닌 숫자로 반환한다.

#### 반복 탐지 전화번호

`GET /api/statistics/repeated-phones?limit=10`

```json
{
  "items": [
    {
      "maskedNumber": "010-3***-8291",
      "detectionCount": 14,
      "lastDetectedAt": "2026-06-10T14:20:00+09:00",
      "blockStatus": "BLOCKED"
    }
  ]
}
```

- 전화번호는 서버에서 마스킹한 값만 대시보드에 반환한다.
- 차단 상태: `BLOCKED`, `MONITORING`

#### 계좌 동결 현황

`GET /api/statistics/account-freezes?limit=10`

```json
{
  "items": [
    {
      "id": 1,
      "bankName": "국민",
      "maskedAccountNumber": "812301-**-***921",
      "requestedAt": "2026-06-10T14:22:00+09:00",
      "status": "COMPLETED"
    }
  ]
}
```

- 계좌번호는 서버에서 마스킹한다.
- 처리 상태: `REQUESTED`, `PROCESSING`, `COMPLETED`, `FAILED`

---

## 4. WebSocket 요구사항

### 연결

- Endpoint: `GET /ws/dashboard`
- 프론트 환경 변수: `VITE_WS_URL=ws://localhost:8080/ws/dashboard`
- 현재 프론트 README는 native WebSocket을 전제로 한다. STOMP를 선택하면 프론트에 STOMP 클라이언트 의존성과 구독 로직이 추가되어야 하므로 팀 내에서 프로토콜을 먼저 확정한다.
- 연결 상태, 끊김, 재연결을 확인할 수 있도록 ping/pong 또는 heartbeat를 지원한다.
- 운영 환경에서는 `wss`를 사용한다.

### 이벤트 envelope

README에 작성된 기존 메시지는 이벤트 종류와 `caseId`가 없어 여러 사건을 동시에 처리할 수 없다. 모든 메시지는 아래 공통 구조를 사용해야 한다.

```json
{
  "eventId": "01JY...",
  "eventType": "CASE_CREATED",
  "caseId": 1,
  "occurredAt": "2026-06-10T14:22:11+09:00",
  "data": {}
}
```

### 필수 서버 -> 대시보드 이벤트

#### `CASE_CREATED`

- 위험도 임계값 이상인 새 사건 생성 시 발행
- `data`에는 사건 목록/상세 표시에 필요한 전체 사건 데이터를 포함
- 대시보드는 수신 후 위험도 내림차순으로 목록에 추가

#### `CASE_UPDATED`

- 위험도, 요약, 키워드, 위치, 경찰서 상태, 사건 상태 등 사건 데이터 변경 시 발행
- 사건 종료 시에도 발행하여 모든 접속 화면에서 제거 또는 완료 처리

#### `PROCESS_UPDATED`

- GPS, SMS, 경찰 통보 도구 실행 상태 변경 시 발행

```json
{
  "eventId": "01JY...",
  "eventType": "PROCESS_UPDATED",
  "caseId": 1,
  "occurredAt": "2026-06-10T14:22:11+09:00",
  "data": {
    "tool": "GPS",
    "status": "COMPLETED",
    "detail": "서울시 강남구",
    "completedAt": "2026-06-10T14:22:11+09:00"
  }
}
```

도구 값: `GPS`, `SMS`, `POLICE`

#### `DASHBOARD_SUMMARY_UPDATED`

- 사건 생성 또는 종료 후 상단 통계 카드가 즉시 갱신되어야 한다면 발행
- 구현하지 않을 경우 프론트가 사건 이벤트 수신 후 `/api/dashboard/summary`를 다시 조회해야 한다.

### 전달 신뢰성

- WebSocket 이벤트는 DB 저장 성공 후 발행한다.
- 재연결 중 누락된 이벤트는 REST 재조회로 복구한다.
- 동일한 `eventId`를 중복 수신해도 문제가 없도록 멱등성을 고려한다.
- 다중 서버 운영 시 인메모리 세션만으로는 이벤트 공유가 되지 않으므로 Redis Pub/Sub, Kafka 등의 브로커 도입을 검토한다.

---

## 5. Flutter STT 및 FastAPI 연동

Dashboard로 보낼 원천 데이터가 Spring Boot에 들어오는 경로가 필요하다. Flutter용 API와 FastAPI 연동 endpoint는 Dashboard API와 분리한다.

### 5.1 전체 처리 흐름

```text
Flutter
  -> 통화 세션 생성
  -> 피해자 WebSocket 연결
  -> STT 청크를 WebSocket으로 지속 전송
Spring Boot
  -> 청크 순서 검증 및 DB 저장
  -> 저장 완료 ACK 전송
  -> 미처리 STT가 설정된 기준 이상 누적되면 요약 작업 생성
  -> FastAPI에 비동기 분석/요약 요청
FastAPI
  -> 위험도, 피해 유형, 키워드, 요약 반환
Spring Boot
  -> 분석 결과 저장
  -> 위험도 80 이상이면 사건 생성 및 CASE_CREATED 발행
  -> 이미 생성된 사건이면 결과 갱신 및 CASE_UPDATED 발행
```

Spring Boot가 STT 원문, 처리 완료 위치, AI 요청 상태의 기준 데이터 소유자가 되어야 한다. Flutter 또는 FastAPI가 누적량과 처리 위치를 각자 관리하면 재시도 시 중복 요약이나 청크 누락이 발생할 수 있다.

### 5.2 Flutter용 통화/STT API

#### 통화 세션 생성

`POST /api/mobile/call-sessions`

요청 예시:

```json
{
  "externalCallId": "device-call-01JY...",
  "deviceId": "device-01",
  "startedAt": "2026-06-10T14:20:00+09:00",
  "phoneNumber": "01012345678",
  "victim": {
    "name": "김OO",
    "age": 71
  }
}
```

응답:

```json
{
  "sessionId": "01JY...",
  "status": "ACTIVE",
  "nextSequence": 1,
  "webSocketUrl": "/ws/v1/victim?sessionId=01JY..."
}
```

- `externalCallId`를 멱등 키로 사용해 같은 통화 세션이 중복 생성되지 않도록 한다.
- 전화번호 등 민감정보는 암호화 저장하고 로그에는 원문을 남기지 않는다.

#### 피해자 WebSocket 연결 및 STT 청크 전송

`WS /ws/v1/victim?sessionId={sessionId}`

Flutter -> Spring Boot:

```json
{
  "eventId": "01JY...",
  "eventType": "TRANSCRIPT_CHUNK",
  "sessionId": "01JY...",
  "occurredAt": "2026-06-10T14:22:11+09:00",
  "data": {
    "chunkId": "01JY...",
    "sequence": 12,
    "text": "검찰 수사관입니다. 지금 안전계좌로 이체하세요.",
    "startedAtMs": 42000,
    "endedAtMs": 47800,
    "isFinal": true
  }
}
```

Spring Boot -> Flutter ACK:

```json
{
  "eventId": "01JY...",
  "eventType": "TRANSCRIPT_ACK",
  "sessionId": "01JY...",
  "occurredAt": "2026-06-10T14:22:11+09:00",
  "data": {
    "chunkId": "01JY...",
    "acceptedSequence": 12,
    "nextSequence": 13,
    "analysisTriggered": false
  }
}
```

처리 요구사항:

- `chunkId` 또는 `(sessionId, sequence)`에 unique 제약을 두어 Flutter 재전송을 멱등 처리한다.
- `sequence`로 STT 순서를 보장하고, 누락된 sequence가 있으면 `TRANSCRIPT_NACK`으로 서버가 기대하는 `nextSequence`를 반환한다.
- Flutter는 `TRANSCRIPT_ACK`를 받지 못한 청크를 로컬 큐에 보관하고 같은 식별자로 재전송한다.
- 빈 텍스트, 최대 길이 초과, 종료된 세션의 청크는 거부한다.
- 서버는 FastAPI 분석 완료를 기다리지 않고 DB 저장 트랜잭션 완료 직후 ACK를 전송한다.
- WebSocket 연결별 처리량 제한, 최대 메시지 크기, 최대 미확인 청크 수를 설정한다.
- STT 원문을 WebSocket으로 Dashboard에 직접 전달하지 않는다.

`isFinal`은 해당 STT 조각이 확정본임을 뜻한다. Flutter STT 엔진이 중간 결과도 전송한다면 중간 결과와 확정 결과의 교체 규칙을 별도로 정의해야 한다. 초기 구현에서는 확정본만 전송하는 것을 권장한다.

#### 통화 세션 종료

피해자 WebSocket으로 `SESSION_COMPLETE` 이벤트를 전송한다.

```json
{
  "eventId": "01JY...",
  "eventType": "SESSION_COMPLETE",
  "sessionId": "01JY...",
  "occurredAt": "2026-06-10T14:32:00+09:00",
  "data": {
    "endedAt": "2026-06-10T14:32:00+09:00",
    "lastTranscriptSequence": 42
  }
}
```

- 남아 있는 미처리 STT가 기준량보다 적더라도 마지막 분석/요약을 요청한다.
- 이미 종료된 세션에 대한 이벤트는 멱등 처리한다.
- 마지막 sequence까지 모두 저장한 뒤 `SESSION_COMPLETE_ACK`를 전송한다.
- 실행 중인 분석 작업과 마지막 분석 요청의 순서를 보장한다.

### 5.3 STT 누적 및 요약 작업 생성 규칙

단순 청크 개수보다는 텍스트 크기와 시간을 기준으로 설정하는 것이 안전하다. STT 엔진에 따라 청크 길이가 크게 달라질 수 있기 때문이다.

권장 설정값:

```yaml
analysis:
  transcript:
    min-characters: 1000
    max-wait-seconds: 30
  fast-api:
    timeout-seconds: 20
    max-retries: 3
```

요약 작업 생성 조건:

- 마지막으로 처리한 청크 이후의 미처리 텍스트가 `min-characters` 이상 누적된 경우
- 또는 마지막 분석 이후 `max-wait-seconds`가 지나고 미처리 텍스트가 존재하는 경우
- 또는 통화 세션 종료 요청을 받은 경우

세션별 처리 규칙:

- 한 통화 세션에서는 FastAPI 분석 요청을 동시에 하나만 실행한다.
- 분석 실행 중 새 STT 청크가 들어오면 계속 저장하고 다음 분석 대상으로 남긴다.
- 각 작업에 처리 범위인 `fromSequence`, `toSequence`를 고정한다.
- 분석 성공 후에만 `lastAnalyzedSequence`를 전진시킨다.
- 분석 실패 시 같은 작업 ID와 같은 sequence 범위로 재시도한다.
- 이전 분석 결과와 새 청크를 함께 FastAPI에 보내 점진적으로 전체 통화 맥락을 갱신한다.
- `(sessionId, fromSequence, toSequence)`에 unique 제약을 두어 같은 범위의 작업이 중복 생성되지 않도록 한다.
- 작업 생성 시 통화 세션 row lock 또는 동등한 동시성 제어를 사용해 여러 서버 인스턴스가 동일 세션을 동시에 분석하지 않도록 한다.

### 5.4 Spring Boot -> FastAPI 분석 요청

`POST {FAST_API_URL}/api/v1/call-analysis`

Spring Boot가 FastAPI를 호출하는 outbound 계약이다.

요청 예시:

```json
{
  "jobId": "01JY...",
  "sessionId": "01JY...",
  "fromSequence": 11,
  "toSequence": 12,
  "transcriptChunks": [
    {
      "sequence": 11,
      "text": "검찰 수사관입니다."
    },
    {
      "sequence": 12,
      "text": "지금 안전계좌로 이체하세요."
    }
  ],
  "previousAnalysis": {
    "risk": 72,
    "types": ["AGENCY_IMPERSONATION"],
    "keywords": ["검찰 수사관"],
    "summary": ["검찰 수사관을 사칭하며 접근"]
  }
}
```

응답 예시:

```json
{
  "jobId": "01JY...",
  "sessionId": "01JY...",
  "risk": 92,
  "types": [
    "AGENCY_IMPERSONATION",
    "ACCOUNT_TRANSFER_INDUCEMENT"
  ],
  "keywords": ["검찰 수사관", "안전계좌", "이체"],
  "summary": [
    "검찰 수사관을 사칭하며 접근",
    "안전계좌로 즉시 이체할 것을 요구"
  ],
  "analyzedToSequence": 12,
  "modelVersion": "voice-phishing-v1"
}
```

처리 요구사항:

- Spring Boot 요청 처리 스레드에서 AI 응답을 기다리지 않고 별도 비동기 작업으로 실행한다.
- `jobId`를 멱등 키로 사용한다.
- 응답의 `jobId`, `sessionId`, `analyzedToSequence`가 요청과 일치하는지 검증한다.
- 위험도는 0~100 범위인지 검증한다.
- 분석 결과와 `modelVersion`을 저장해 추적 가능하게 한다.
- 위험도가 처음으로 80 이상이 되면 사건을 생성하고 `CASE_CREATED`를 발행한다.
- 이미 사건이 존재하면 위험도, 유형, 키워드, 요약을 최신 결과로 갱신하고 `CASE_UPDATED`를 발행한다.
- 후속 분석에서 위험도가 낮아져도 진행 중 사건을 자동 종료하지 않는다.

FastAPI 호출 방식은 초기 구현에서는 비동기 HTTP 요청과 재시도로 충분하다. 처리량이 증가하거나 장애 격리가 필요하면 RabbitMQ/Kafka 기반 작업 큐로 전환한다.

### 5.5 도구 실행 결과 수신

`POST /internal/cases/{caseId}/process-events`

- FastAPI의 GPS, SMS, 경찰 통보 결과를 저장
- 저장 후 `PROCESS_UPDATED` 발행
- 단계 순서를 강제할지, 비동기 완료를 허용할지 정책 결정 필요

내부 API에는 서비스 간 인증이 반드시 필요하다. 최소 API key 또는 서명 검증을 적용하고, 운영 환경에서는 네트워크 접근도 제한한다.

---

## 6. 권장 도메인 및 저장 데이터

### 핵심 테이블

- `cases`
  - 사건 기본 정보, 위험도, 상태, 탐지/종료 시각, 위치, 외부 식별자
- `call_sessions`
  - Flutter 통화 세션, 외부 통화 식별자, 상태, 마지막 수신/분석 sequence
- `transcript_chunks`
  - 통화 세션별 STT 원문, 순서, 구간 시각, 확정 여부
- `analysis_jobs`
  - FastAPI 요청 범위, 작업 상태, 재시도 횟수, 모델 버전
- `analysis_results`
  - 작업별 위험도, 유형, 키워드, 요약 결과
- `case_types`
  - 사건별 피해 유형
- `case_keywords`
  - 사건별 탐지 키워드
- `case_summaries`
  - 순서를 가진 AI 요약 문장
- `case_process_events`
  - GPS/SMS/POLICE 실행 상태와 상세 결과
- `police_stations`
  - 경찰서명, 위치, 상태
- `detected_phones`
  - 탐지 전화번호와 차단 상태
- `mentioned_accounts`
  - 언급 계좌와 동결 요청 상태

원본 전화번호와 계좌번호는 암호화 저장하고, API 응답에는 마스킹 값만 노출한다.

### 주요 상태 전이

```text
사건: IN_PROGRESS -> COMPLETED
통화 세션: ACTIVE -> COMPLETED
분석 작업: PENDING -> PROCESSING -> COMPLETED | FAILED
프로세스: PENDING -> IN_PROGRESS -> COMPLETED | FAILED
계좌 동결: REQUESTED -> PROCESSING -> COMPLETED | FAILED
```

완료된 사건을 다시 진행 중으로 돌리는 기능은 현재 프론트 요구사항에 없으므로 허용하지 않는다.

---

## 7. Spring Boot 공통 구현 항목

### 의존성 및 설정

- WebSocket 사용을 위한 Spring WebSocket 의존성 추가
- Bean Validation 의존성 및 요청 DTO 검증
- Dashboard origin에 대한 CORS/WebSocket origin 설정
- 환경 변수 기반 DB 및 외부 서비스 설정
- OpenAPI에 REST endpoint와 오류 응답 명세 추가

### 공통 응답과 오류 처리

- 전역 예외 처리 구현
- 오류 응답에 `code`, `message`, `timestamp`, 필요 시 `fieldErrors` 포함
- 주요 오류:
  - `CASE_NOT_FOUND`
  - `INVALID_CASE_STATUS_TRANSITION`
  - `INVALID_PROCESS_EVENT`
  - `DUPLICATE_EXTERNAL_EVENT`
  - `INVALID_DATE_RANGE`
  - `CALL_SESSION_NOT_FOUND`
  - `CALL_SESSION_COMPLETED`
  - `TRANSCRIPT_SEQUENCE_MISMATCH`
  - `ANALYSIS_JOB_FAILED`

### 데이터 및 보안

- 이름, 전화번호, 계좌번호 등 개인정보는 로그에 원문을 남기지 않는다.
- 통계 조회 결과에도 마스킹을 적용한다.
- 내부 연동 API 인증과 Dashboard API 인증/인가 정책을 분리한다.
- 사건 생성, 종료, 프로세스 변경은 감사 로그를 남긴다.

### 성능

- 사건 목록: `status`, `risk`, `detectedAt` 인덱스
- STT 처리: `(sessionId, sequence)`, 분석 작업의 `status`, `createdAt` 인덱스
- 통계: 기간 조건에 사용되는 `detectedAt`, `requestedAt`, 키워드 인덱스
- 통계 집계가 느려지면 캐시 또는 집계 테이블 사용
- 목록/통계 테이블 API에 `limit`과 최대값 적용

---

## 8. 구현 우선순위

### P0: 실시간 모니터링 최소 동작

- 사건 Entity/Repository/Service
- Flutter 통화 세션 API 및 STT 양방향 WebSocket
- STT 누적 기준 판정 및 분석 작업 관리
- Spring Boot -> FastAPI 비동기 분석 요청과 재시도
- `GET /api/cases`
- `GET /api/dashboard/summary`
- `PATCH /api/cases/{caseId}/status`
- `/ws/dashboard` 연결
- `CASE_CREATED`, `CASE_UPDATED`, `PROCESS_UPDATED`
- FastAPI 분석 결과 및 도구 결과 저장 경로
- CORS, validation, 예외 처리

### P1: 통계 화면

- 통계용 전화번호/계좌 데이터 저장
- `/api/statistics/summary`
- `/api/statistics/keywords`
- `/api/statistics/age-groups`
- `/api/statistics/repeated-phones`
- `/api/statistics/account-freezes`

### P2: 운영 안정성

- 인증/인가
- 감사 로그
- WebSocket heartbeat와 재연결 복구 검증
- 다중 인스턴스 이벤트 브로커
- 통계 캐시 및 성능 최적화

---

## 9. 완료 조건

- 대시보드 진입 시 현재 진행 중 사건과 상단 요약이 표시된다.
- Flutter가 같은 STT 청크를 재전송해도 중복 저장 또는 중복 분석되지 않는다.
- STT 청크 순서 누락을 서버가 감지하고 Flutter가 복구할 수 있다.
- 설정된 텍스트량 또는 대기 시간이 충족되면 FastAPI 분석 요청이 한 번 생성된다.
- 분석 중 새 STT가 들어와도 유실되지 않고 다음 분석에 포함된다.
- 통화 종료 시 기준량 미만의 마지막 STT도 분석된다.
- 위험도 80 이상 사건이 생성되면 새로고침 없이 목록에 나타난다.
- GPS/SMS/POLICE 결과가 해당 `caseId`의 체크리스트에만 반영된다.
- 사건 종료 요청 후 모든 접속 대시보드에서 상태가 일관되게 갱신된다.
- WebSocket 연결이 끊겼다가 복구되어도 REST 재조회로 누락 상태가 복원된다.
- 통계 화면의 모든 목업 데이터가 REST 응답으로 대체 가능하다.
- 전화번호와 계좌번호는 API와 로그에서 마스킹된다.
- Swagger에서 모든 REST API와 요청/응답 예시를 확인할 수 있다.
