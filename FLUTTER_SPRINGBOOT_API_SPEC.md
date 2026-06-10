# SSAIREN Flutter - Spring Boot API 명세

## 1. 문서 목적

`SSAIREN_킥오프_스펙문서.docx`와 현재 확정된 STT 처리 방향을 기준으로, Flutter 앱과 Spring Boot 사이에 필요한 API를 정의한다.

Flutter 앱은 역할이 두 가지다.

- 피해자 앱: 통화 세션 관리, STT 텍스트 전송, 위험도 확인, 대응 실행 동의, 위치 전송, 실행 결과 수신
- 보호자 앱: FCM 알림 수신, 위치 전송, 안심 메시지 전송

### 원본 스펙에서 변경된 사항

원본 스펙은 Flutter/Android가 5초 WAV 파일을 `/analyze`로 전송하고 서버가 Whisper STT를 수행하는 구조다.

현재 확정 방향은 Flutter가 STT를 수행하고 텍스트 청크를 Spring Boot에 전송하는 구조이므로 다음처럼 변경한다.

| 원본 스펙 | 적용 명세 |
| --- | --- |
| `POST /analyze`로 WAV 전송 | 피해자 WebSocket으로 `TRANSCRIPT_CHUNK` 전송 |
| `/analyze` 요청마다 위험도 즉시 응답 | WebSocket ACK 수신 후 AI 분석 결과도 같은 연결로 비동기 수신 |
| `is_triggered` 전역 변수 | 통화 세션별 `triggerStatus` 관리 |
| 식별자 없는 WebSocket | 모든 이벤트에 `eventId`, `sessionId`, `eventType` 포함 |

Spring Boot와 FastAPI 사이의 API는 이 문서 범위에서 제외한다.

---

## 2. 공통 규칙

### Base URL

```text
https://{server}/api/v1
```

### 인증

초기 데모 이후 실제 서비스에서는 모든 요청에 인증이 필요하다.

```http
Authorization: Bearer {accessToken}
X-Device-Id: {deviceId}
```

피해자와 보호자 권한을 구분한다.

- `VICTIM`: 피해자 앱 API 호출 가능
- `GUARDIAN`: 보호자 앱 API 호출 가능

### 공통 형식

- Content-Type: `application/json`
- 시간: ISO-8601 with timezone
- ID: UUID 또는 ULID 문자열
- 위험도: 정수 `0~100`
- 동일 요청 재전송이 가능한 API에는 `Idempotency-Key` 사용

### 공통 오류 응답

```json
{
  "code": "TRANSCRIPT_SEQUENCE_MISMATCH",
  "message": "Expected sequence is 13.",
  "timestamp": "2026-06-10T15:20:00+09:00",
  "details": {
    "expectedSequence": 13
  }
}
```

주요 HTTP 상태:

- `400`: 요청 형식 또는 값 오류
- `401`: 인증 실패
- `403`: 역할 또는 세션 접근 권한 없음
- `404`: 대상 세션/요청 없음
- `409`: sequence 불일치, 중복 상태 전이
- `422`: 처리할 수 없는 STT 데이터
- `500`: 서버 내부 오류

WebSocket 메시지 처리 오류는 HTTP 상태 대신 `TRANSCRIPT_NACK` 또는 해당 이벤트 전용 실패 메시지로 전달한다.

---

## 3. API 목록

| 구분 | Method | Endpoint | 호출 앱 | 목적 |
| --- | --- | --- | --- | --- |
| 기기 등록 | `PUT` | `/devices/fcm-token` | 피해자/보호자 | FCM 토큰 등록 및 갱신 |
| 기기 해제 | `DELETE` | `/devices/fcm-token` | 피해자/보호자 | 로그아웃 시 FCM 토큰 비활성화 |
| 통화 시작 | `POST` | `/call-sessions` | 피해자 | 통화 모니터링 세션 생성 |
| 세션 상태 조회 | `GET` | `/call-sessions/{sessionId}` | 피해자 | 재접속 시 현재 상태 복구 |
| 트리거 동의 | `POST` | `/call-sessions/{sessionId}/trigger/confirm` | 피해자 | 대응 도구 실행 동의 |
| 트리거 거절 | `POST` | `/call-sessions/{sessionId}/trigger/reject` | 피해자 | 대응 도구 실행 거절 |
| 피해자 위치 전송 | `POST` | `/call-sessions/{sessionId}/victim-location` | 피해자 | 현재 위치 서버 전송 |
| 보호자 위치 전송 | `POST` | `/guardian-requests/{requestId}/location` | 보호자 | 납치협박 시 보호자 위치 전송 |
| 보호자 응답 전송 | `POST` | `/guardian-requests/{requestId}/response` | 보호자 | 피해자에게 안심 메시지 전송 |
| 보호자 요청 조회 | `GET` | `/guardian-requests/{requestId}` | 보호자 | FCM 진입 후 요청 상세 조회 |
| 피해자 양방향 연결 | `WS` | `/ws/v1/victim?sessionId={sessionId}` | 피해자 | STT·종료 전송, ACK·분석·도구 결과 수신 |
| 보호자 실시간 연결 | `WS` | `/ws/v1/guardian` | 보호자 | FCM 실패 시 알림 수신 폴백 |

---

## 4. 기기 및 FCM API

### 4.1 FCM 토큰 등록

`PUT /api/v1/devices/fcm-token`

앱 실행, 로그인, FCM 토큰 갱신 시 호출한다.

요청:

```json
{
  "deviceId": "device-01JY...",
  "fcmToken": "firebase-token",
  "platform": "ANDROID",
  "appRole": "GUARDIAN"
}
```

응답:

```json
{
  "deviceId": "device-01JY...",
  "registered": true,
  "updatedAt": "2026-06-10T15:20:00+09:00"
}
```

처리 요구사항:

- 같은 기기의 토큰 갱신은 기존 값을 덮어쓴다.
- 보호자 위치 요청은 앱이 꺼져 있을 수 있으므로 FCM을 기본 전달 방식으로 사용한다.

### 4.2 FCM 토큰 비활성화

`DELETE /api/v1/devices/fcm-token`

요청:

```json
{
  "deviceId": "device-01JY...",
  "fcmToken": "firebase-token"
}
```

응답: `204 No Content`

로그아웃하거나 더 이상 알림을 받지 않는 기기에서 호출한다.

---

## 5. 피해자 앱 API

### 5.1 통화 세션 생성

`POST /api/v1/call-sessions`

Header:

```http
Idempotency-Key: {externalCallId}
```

요청:

```json
{
  "externalCallId": "device-call-01JY...",
  "startedAt": "2026-06-10T15:20:00+09:00",
  "counterpartPhoneNumber": "01012345678"
}
```

응답 `201 Created`:

```json
{
  "sessionId": "session-01JY...",
  "status": "ACTIVE",
  "triggerStatus": "NOT_TRIGGERED",
  "nextTranscriptSequence": 1,
  "webSocketUrl": "/ws/v1/victim?sessionId=session-01JY..."
}
```

처리 요구사항:

- 같은 `externalCallId` 요청은 기존 세션을 반환한다.
- 상대 전화번호는 암호화 저장하고 로그에는 마스킹한다.
- 세션 생성 후 피해자 WebSocket 연결을 수립한다.

### 5.2 세션 상태 조회

`GET /api/v1/call-sessions/{sessionId}`

앱 재시작 또는 WebSocket 재연결 시 상태 복구용이다.

응답:

```json
{
  "sessionId": "session-01JY...",
  "status": "ACTIVE",
  "triggerStatus": "AWAITING_CONFIRMATION",
  "nextTranscriptSequence": 13,
  "latestAnalysis": {
    "risk": 84,
    "type": "AGENCY_IMPERSONATION",
    "summary": [
      "검찰 수사관을 사칭하며 접근",
      "계좌 이체를 요구"
    ],
    "analyzedToSequence": 12,
    "analyzedAt": "2026-06-10T15:21:00+09:00"
  },
  "tools": [
    {
      "tool": "SMS",
      "status": "PENDING",
      "detail": null
    },
    {
      "tool": "GPS",
      "status": "PENDING",
      "detail": null
    },
    {
      "tool": "POLICE",
      "status": "PENDING",
      "detail": null
    }
  ]
}
```

### 5.3 STT 전송 및 통화 종료

STT 청크와 통화 종료는 피해자 WebSocket의 클라이언트 -> 서버 메시지로 전송한다. 상세 메시지 계약은 `8. WebSocket 명세`를 따른다.

처리 요구사항:

- Flutter STT 엔진이 확정한 텍스트만 `TRANSCRIPT_CHUNK`로 전송한다.
- 서버는 청크를 DB에 저장한 후 `TRANSCRIPT_ACK`를 보낸다.
- Flutter는 ACK를 받지 못한 청크를 로컬 큐에 유지하고 재연결 후 같은 `chunkId`로 재전송한다.
- 통화 종료 시 Flutter는 마지막 청크 ACK를 확인한 후 `SESSION_COMPLETE`를 전송한다.
- REST 통화 종료 API를 별도로 두지 않고 WebSocket 메시지로 처리한다.

### 5.4 트리거 대응 동의

`POST /api/v1/call-sessions/{sessionId}/trigger/confirm`

위험도 임계값 이상으로 `TRIGGER_PLAN_REQUIRED` 이벤트를 받은 뒤 사용자가 `[네]`를 누르면 호출한다.

요청:

```json
{
  "planId": "plan-01JY...",
  "confirmedAt": "2026-06-10T15:22:00+09:00",
  "approvedTools": ["SMS", "GPS", "POLICE"]
}
```

응답 `202 Accepted`:

```json
{
  "sessionId": "session-01JY...",
  "triggerStatus": "EXECUTING",
  "tools": [
    { "tool": "SMS", "status": "PENDING" },
    { "tool": "GPS", "status": "PENDING" },
    { "tool": "POLICE", "status": "PENDING" }
  ]
}
```

처리 요구사항:

- 세션당 최초 동의 요청만 도구 실행을 시작한다.
- 같은 `planId` 재요청은 현재 상태를 반환한다.
- 승인된 도구만 실행한다.
- 실행 결과는 피해자 WebSocket으로 전달한다.

### 5.5 트리거 대응 거절

`POST /api/v1/call-sessions/{sessionId}/trigger/reject`

요청:

```json
{
  "planId": "plan-01JY...",
  "rejectedAt": "2026-06-10T15:22:00+09:00",
  "reason": "USER_REJECTED"
}
```

응답:

```json
{
  "sessionId": "session-01JY...",
  "triggerStatus": "REJECTED"
}
```

거절 후에도 STT 분석을 계속할지 중단할지는 정책 결정이 필요하다. 위험 상황 추적을 위해 분석은 통화 종료까지 계속하는 것을 권장한다.

### 5.6 피해자 위치 전송

`POST /api/v1/call-sessions/{sessionId}/victim-location`

요청:

```json
{
  "latitude": 37.4979,
  "longitude": 127.0276,
  "accuracyMeters": 18.5,
  "capturedAt": "2026-06-10T15:22:10+09:00"
}
```

응답:

```json
{
  "accepted": true,
  "locationName": "서울 강남구"
}
```

---

## 6. 보호자 앱 API

### 6.1 보호자 요청 조회

`GET /api/v1/guardian-requests/{requestId}`

보호자 앱이 FCM 알림을 눌러 진입한 뒤 상세 내용을 조회한다.

응답:

```json
{
  "requestId": "guardian-request-01JY...",
  "requestType": "LOCATION_AND_MESSAGE",
  "status": "PENDING",
  "victim": {
    "name": "김OO",
    "relationship": "부"
  },
  "message": "납치 협박 의심 통화가 감지되었습니다. 현재 위치와 안심 메시지를 보내주세요.",
  "expiresAt": "2026-06-10T15:32:00+09:00"
}
```

### 6.2 보호자 위치 전송

`POST /api/v1/guardian-requests/{requestId}/location`

요청:

```json
{
  "latitude": 37.5012,
  "longitude": 127.0351,
  "accuracyMeters": 12.0,
  "capturedAt": "2026-06-10T15:23:00+09:00"
}
```

응답:

```json
{
  "accepted": true,
  "status": "LOCATION_RECEIVED"
}
```

처리 요구사항:

- 요청 대상 보호자만 전송할 수 있다.
- 만료된 요청은 `410 Gone`으로 응답한다.
- 수신한 위치는 피해자 앱과 경찰 Dashboard에 전달할 수 있다.

### 6.3 안심 메시지 전송

`POST /api/v1/guardian-requests/{requestId}/response`

요청:

```json
{
  "message": "아버지, 저는 안전합니다. 송금하지 마세요.",
  "sentAt": "2026-06-10T15:23:20+09:00"
}
```

응답:

```json
{
  "accepted": true,
  "status": "RESPONDED"
}
```

처리 요구사항:

- 메시지 최대 길이를 제한한다.
- 피해자 앱에 `GUARDIAN_RESPONSE_RECEIVED` 이벤트를 전송한다.

---

## 7. FCM 메시지

FCM은 보호자 앱이 종료되었거나 백그라운드 상태여도 알림을 전달하기 위한 기본 방식이다.

### 보호자 위치 및 안심 메시지 요청

```json
{
  "notification": {
    "title": "SSAIREN 긴급 확인 요청",
    "body": "가족의 통화에서 납치 협박 위험이 감지되었습니다."
  },
  "data": {
    "eventType": "GUARDIAN_ACTION_REQUIRED",
    "requestId": "guardian-request-01JY...",
    "requestType": "LOCATION_AND_MESSAGE"
  }
}
```

FCM `data`에는 민감정보를 직접 포함하지 않고, 앱이 인증 후 상세 API를 조회하도록 한다.

---

## 8. WebSocket 명세

### 8.1 피해자 앱 연결

```text
wss://{server}/ws/v1/victim?sessionId={sessionId}
```

인증 토큰 전달 방식은 handshake header 또는 단기 WebSocket ticket 중 하나로 확정해야 한다.

이 연결은 양방향으로 사용한다.

- Flutter -> Spring Boot: STT 청크, 통화 종료, heartbeat
- Spring Boot -> Flutter: 청크 ACK/NACK, 분석 결과, 트리거 계획, 도구 결과

연결 직후 서버는 현재 수신 위치를 전달한다.

```json
{
  "eventId": "event-01JY...",
  "eventType": "SESSION_READY",
  "sessionId": "session-01JY...",
  "occurredAt": "2026-06-10T15:20:00+09:00",
  "data": {
    "nextTranscriptSequence": 1
  }
}
```

### 8.2 보호자 앱 연결

```text
wss://{server}/ws/v1/guardian
```

보호자 WebSocket은 FCM 구현 실패 시 폴백이며, 앱이 실행 중일 때만 신뢰할 수 있다.

### 공통 이벤트 envelope

```json
{
  "eventId": "event-01JY...",
  "eventType": "ANALYSIS_UPDATED",
  "sessionId": "session-01JY...",
  "occurredAt": "2026-06-10T15:21:00+09:00",
  "data": {}
}
```

### 피해자 앱 송신 이벤트

#### `TRANSCRIPT_CHUNK`

```json
{
  "eventId": "chunk-01JY...",
  "eventType": "TRANSCRIPT_CHUNK",
  "sessionId": "session-01JY...",
  "occurredAt": "2026-06-10T15:20:47+09:00",
  "data": {
    "chunkId": "chunk-01JY...",
    "sequence": 12,
    "text": "검찰 수사관입니다. 지금 안전계좌로 이체하세요.",
    "startedAtMs": 42000,
    "endedAtMs": 47800,
    "isFinal": true
  }
}
```

처리 순서:

1. 연결의 인증 사용자와 `sessionId` 소유권을 검증한다.
2. `chunkId` 또는 `(sessionId, sequence)` 중복 여부를 검증한다.
3. 서버가 기대하는 `nextTranscriptSequence`와 비교한다.
4. 유효한 청크를 DB에 저장한다.
5. 누적량과 마지막 분석 시간을 기준으로 FastAPI 분석 작업 생성 여부를 판단한다.
6. 저장 트랜잭션 완료 후 `TRANSCRIPT_ACK`를 전송한다.

서버는 WebSocket 메시지 처리 스레드에서 FastAPI 결과를 기다리지 않는다.

#### `SESSION_COMPLETE`

```json
{
  "eventId": "event-01JY...",
  "eventType": "SESSION_COMPLETE",
  "sessionId": "session-01JY...",
  "occurredAt": "2026-06-10T15:32:00+09:00",
  "data": {
    "endedAt": "2026-06-10T15:32:00+09:00",
    "lastTranscriptSequence": 42
  }
}
```

- 서버가 `lastTranscriptSequence`까지 저장했으면 세션을 `COMPLETING`으로 변경하고 마지막 분석을 요청한다.
- 누락된 청크가 있으면 `TRANSCRIPT_NACK`으로 기대 sequence를 반환한다.
- 중복 종료 이벤트는 멱등 처리한다.

#### `PING`

클라이언트는 연결 생존 확인을 위해 주기적으로 ping 메시지 또는 WebSocket protocol ping frame을 보낸다.

### 피해자 앱 수신 ACK 이벤트

#### `TRANSCRIPT_ACK`

```json
{
  "eventId": "event-01JY...",
  "eventType": "TRANSCRIPT_ACK",
  "sessionId": "session-01JY...",
  "occurredAt": "2026-06-10T15:20:47+09:00",
  "data": {
    "chunkId": "chunk-01JY...",
    "acceptedSequence": 12,
    "nextTranscriptSequence": 13,
    "analysisQueued": false
  }
}
```

Flutter는 ACK를 받은 청크만 로컬 큐에서 제거한다.

#### `TRANSCRIPT_NACK`

```json
{
  "eventId": "event-01JY...",
  "eventType": "TRANSCRIPT_NACK",
  "sessionId": "session-01JY...",
  "occurredAt": "2026-06-10T15:20:47+09:00",
  "data": {
    "chunkId": "chunk-01JY...",
    "reason": "SEQUENCE_MISMATCH",
    "expectedSequence": 12
  }
}
```

NACK 사유: `SEQUENCE_MISMATCH`, `DUPLICATE_CONFLICT`, `INVALID_PAYLOAD`, `SESSION_COMPLETED`, `SERVER_BUSY`

#### `SESSION_COMPLETE_ACK`

```json
{
  "eventId": "event-01JY...",
  "eventType": "SESSION_COMPLETE_ACK",
  "sessionId": "session-01JY...",
  "occurredAt": "2026-06-10T15:32:00+09:00",
  "data": {
    "status": "COMPLETING",
    "finalAnalysisQueued": true
  }
}
```

### 피해자 앱 수신 이벤트

#### `ANALYSIS_UPDATED`

통화 중 모니터링 화면의 위험도와 유형을 갱신한다.

```json
{
  "eventId": "event-01JY...",
  "eventType": "ANALYSIS_UPDATED",
  "sessionId": "session-01JY...",
  "occurredAt": "2026-06-10T15:21:00+09:00",
  "data": {
    "risk": 72,
    "type": "AGENCY_IMPERSONATION",
    "analyzedToSequence": 12
  }
}
```

#### `TRIGGER_PLAN_REQUIRED`

위험도 임계값 이상일 때 `[네] / [아니요]` 확인 화면을 표시한다.

```json
{
  "eventId": "event-01JY...",
  "eventType": "TRIGGER_PLAN_REQUIRED",
  "sessionId": "session-01JY...",
  "occurredAt": "2026-06-10T15:22:00+09:00",
  "data": {
    "planId": "plan-01JY...",
    "risk": 84,
    "type": "AGENCY_IMPERSONATION",
    "tools": ["SMS", "GPS", "POLICE"],
    "message": "보이스피싱이 감지됐습니다. 다음 대응을 실행할까요?"
  }
}
```

#### `TOOL_STATUS_UPDATED`

```json
{
  "eventId": "event-01JY...",
  "eventType": "TOOL_STATUS_UPDATED",
  "sessionId": "session-01JY...",
  "occurredAt": "2026-06-10T15:22:10+09:00",
  "data": {
    "tool": "SMS",
    "status": "COMPLETED",
    "detail": "보호자에게 알림을 전송했습니다."
  }
}
```

도구 상태: `PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`

#### `VICTIM_LOCATION_REQUIRED`

GPS 도구 실행 시 피해자 앱에 현재 위치 전송을 요청한다.

```json
{
  "eventId": "event-01JY...",
  "eventType": "VICTIM_LOCATION_REQUIRED",
  "sessionId": "session-01JY...",
  "occurredAt": "2026-06-10T15:22:05+09:00",
  "data": {
    "expiresAt": "2026-06-10T15:23:05+09:00"
  }
}
```

수신한 Flutter 앱은 위치 권한과 GPS 상태를 확인한 뒤 피해자 위치 전송 API를 호출한다.

#### `GUARDIAN_RESPONSE_RECEIVED`

```json
{
  "eventId": "event-01JY...",
  "eventType": "GUARDIAN_RESPONSE_RECEIVED",
  "sessionId": "session-01JY...",
  "occurredAt": "2026-06-10T15:23:20+09:00",
  "data": {
    "message": "아버지, 저는 안전합니다. 송금하지 마세요."
  }
}
```

#### `GUARDIAN_LOCATION_RECEIVED`

납치협박 유형에서 KakaoMap에 보호자 위치를 표시한다.

```json
{
  "eventId": "event-01JY...",
  "eventType": "GUARDIAN_LOCATION_RECEIVED",
  "sessionId": "session-01JY...",
  "occurredAt": "2026-06-10T15:23:00+09:00",
  "data": {
    "latitude": 37.5012,
    "longitude": 127.0351,
    "accuracyMeters": 12.0
  }
}
```

### 보호자 앱 폴백 이벤트

#### `GUARDIAN_ACTION_REQUIRED`

FCM의 보호자 요청과 동일한 `requestId`를 전달한다. 상세 내용은 REST로 조회한다.

---

## 9. 상태 모델

### 통화 세션

```text
ACTIVE -> COMPLETING -> COMPLETED
ACTIVE -> FAILED
```

### 트리거

```text
NOT_TRIGGERED
  -> AWAITING_CONFIRMATION
  -> EXECUTING
  -> COMPLETED | PARTIALLY_FAILED

AWAITING_CONFIRMATION -> REJECTED
```

트리거는 통화 세션당 한 번만 생성한다. 추가 분석에서 위험도가 다시 임계값을 넘어도 새 트리거를 생성하지 않는다.

### 보호자 요청

```text
PENDING -> LOCATION_RECEIVED -> RESPONDED
PENDING -> RESPONDED
PENDING -> EXPIRED
```

---

## 10. Flutter 구현 요구사항

- STT 확정 청크에 단조 증가하는 `sequence`와 고유 `chunkId`를 부여한다.
- `TRANSCRIPT_ACK`를 받기 전까지 청크를 로컬 영속 큐에 보관한다.
- 네트워크 복구 후 `SESSION_READY.nextTranscriptSequence`부터 같은 `chunkId`로 재전송한다.
- 한 세션에서 STT WebSocket 메시지를 sequence 순서대로 전송한다.
- 서버 과부하를 막기 위해 ACK 없이 전송 가능한 최대 청크 수를 제한한다.
- 마지막 청크 ACK 후 `SESSION_COMPLETE`를 전송한다.
- WebSocket 재연결 후 `GET /call-sessions/{sessionId}`로 상태를 복구한다.
- 피해자 앱은 트리거 확인 화면에서 중복 버튼 입력을 방지한다.
- 보호자 앱은 FCM 수신 후 `requestId`로 상세 API를 조회한다.
- 위치 권한 거절, GPS 비활성화, FCM 토큰 만료를 사용자에게 표시하고 서버에 실패 상태를 전달할 수 있어야 한다.

---

## 11. Spring Boot 구현 요구사항

- `externalCallId`, `chunkId`, `(sessionId, sequence)`, `planId`를 이용해 멱등성을 보장한다.
- WebSocket STT 처리에서 FastAPI 응답을 기다리지 않는다.
- 세션별 마지막 수신 sequence와 마지막 분석 sequence를 관리한다.
- 세션별 AI 분석 작업은 동시에 하나만 실행한다.
- 청크 저장 트랜잭션이 완료된 후에만 `TRANSCRIPT_ACK`를 전송한다.
- WebSocket 연결별 처리량 제한과 최대 메시지 크기를 설정한다.
- 연결 종료 후에도 저장된 세션 상태를 유지해 재연결을 허용한다.
- 위험도 임계값은 설정값으로 관리하며 기본값은 `80`이다.
- 트리거 동의 후 도구 실행 상태를 저장하고 WebSocket으로 전달한다.
- FCM 전송 실패를 기록하고 필요 시 보호자 WebSocket 폴백을 사용한다.
- 위치, 전화번호, STT 원문은 민감정보로 분류하고 로그에 원문을 남기지 않는다.

---

## 12. 구현 전 확정 필요 사항

다음 항목은 원본 스펙만으로 확정할 수 없다.

1. 피해자와 보호자 계정 연결 및 인증 방식
2. Flutter에서 사용할 STT 엔진과 중간 결과 전송 여부
3. AI 분석 실행 기준: 누적 글자 수와 최대 대기 시간
4. 트리거 거절 후 분석 지속 여부
5. 실제 SMS 발송 여부와 FCM만 사용할지 여부
6. GPS 권한 거절 또는 위치 수신 실패 시 처리 방식
7. WebSocket 인증 토큰 전달 방식
8. 보호자 위치와 안심 메시지 요청 만료 시간
