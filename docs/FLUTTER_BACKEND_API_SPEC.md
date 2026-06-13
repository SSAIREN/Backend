# Flutter - Backend API Specification

## 1. 문서 목적

이 문서는 Flutter 앱이 Spring Boot Backend와 통신할 때 따라야 하는 API 규격을 정리한 문서다.

현재 MVP 플로우는 아래 2단계로 나뉜다.

1. REST 초기 분석 단계
- 통화 시작 직후 Flutter가 5초 단위 STT 텍스트를 REST API로 보낸다.
- Backend는 FastAPI 분석 서버에 전달한 뒤 결과를 REST 응답으로 돌려준다.

2. WebSocket 실시간 감시 단계
- REST 분석 결과에서 `shouldOpenWebSocket=true`가 내려오면 Flutter가 WebSocket으로 전환한다.
- 이후 5초 단위 STT 텍스트는 WebSocket 이벤트로 보낸다.
- Backend는 FastAPI 실시간 분석 결과를 WebSocket 이벤트로 반환한다.

## 2. 공통 정보

- Base URL: `http://{backend-host}:8080`
- Content-Type: `application/json`
- 시간 포맷: `ISO-8601 OffsetDateTime`

예시:

`2026-06-10T15:20:00+09:00`

## 3. MVP 사용자 식별 방식

MVP 단계에서는 Flutter가 `userId`를 더미값으로 고정해서 보낸다.

- 권장 값: `1001`
- Backend는 서버 시작 시 더미 사용자와 보호자 데이터를 미리 적재한다.
- 통화 세션 생성 시 이 `userId`를 기준으로 피해자 정보를 연결한다.

## 4. 전체 통신 흐름

### 4-1. 통화 시작 직후

1. Flutter가 통화 시작 시 세션 생성 API를 호출한다.
2. Backend가 `sessionId`를 발급한다.
3. Flutter는 5초 단위 STT를 REST 분석 API로 보낸다.
4. Backend는 FastAPI REST 분석 endpoint로 전달한다.
5. Backend는 분석 결과를 Flutter REST 응답으로 반환한다.
6. Flutter는 `shouldOpenWebSocket` 값을 보고 다음 단계를 결정한다.

### 4-2. 위험도 상승 이후

1. Flutter가 `/ws/v1/victim?sessionId={sessionId}` 로 WebSocket 연결한다.
2. Backend는 `SESSION_READY` 이벤트로 다음 sequence를 알려준다.
3. Flutter는 5초 단위 STT를 `TRANSCRIPT_CHUNK` 이벤트로 전송한다.
4. Backend는 FastAPI 실시간 분석 endpoint로 전달한다.
5. Backend는 `TRANSCRIPT_ACK`, `ANALYSIS_RESULT` 등을 Flutter에 보낸다.
6. 통화 종료 시 Flutter는 `SESSION_COMPLETE` 이벤트를 보낸다.

## 5. 세션 생성

### `POST /api/mobile/call-sessions`

통화 시작 시 가장 먼저 호출하는 API다.

### 요청 본문 예시

```json
{
  "userId": 1001,
  "externalCallId": "device-call-001",
  "deviceId": "victim-device-001",
  "startedAt": "2026-06-10T15:20:00+09:00",
  "phoneNumber": "01012345678",
  "victim": {
    "name": "김영희",
    "age": 71
  }
}
```

### 요청 필드

- `userId`: MVP 더미 사용자 식별값
- `externalCallId`: Flutter가 생성하는 통화 고유 ID
- `deviceId`: 단말 식별값
- `startedAt`: 통화 시작 시각
- `phoneNumber`: 상대 전화번호
- `victim.name`: 피해자 이름
- `victim.age`: 피해자 나이

### 응답 예시

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ACTIVE",
  "nextTranscriptSequence": 1,
  "accumulatedTranscriptCharacters": 0,
  "startedAt": "2026-06-10T15:20:00+09:00",
  "endedAt": null,
  "webSocketUrl": "/ws/v1/victim?sessionId=550e8400-e29b-41d4-a716-446655440000"
}
```

### 응답 필드

- `sessionId`: Backend가 발급한 세션 ID
- `status`: 현재 세션 상태
- `nextTranscriptSequence`: 다음 STT 청크에 사용할 sequence
- `accumulatedTranscriptCharacters`: 현재까지 누적된 transcript 길이
- `startedAt`: 통화 시작 시각
- `endedAt`: 통화 종료 시각, 진행 중이면 `null`
- `webSocketUrl`: 실시간 감시 단계에서 사용할 WebSocket 경로

## 6. 세션 상태 조회

### `GET /api/mobile/call-sessions/{sessionId}`

앱 재진입, 네트워크 복구, WebSocket 재연결 전에 현재 세션 상태를 복원할 때 사용한다.

### 응답

세션 생성 응답과 동일한 구조를 반환한다.

## 7. 1단계: REST 초기 STT 분석

### `POST /api/mobile/call-sessions/{sessionId}/transcripts/analyze`

Flutter가 5초 단위의 확정 STT 텍스트를 보내는 API다.

### 요청 본문 예시

```json
{
  "chunkId": "chunk-001",
  "sequence": 1,
  "text": "검찰 수사관입니다. 안전계좌로 이체하세요.",
  "startedAtMs": 0,
  "endedAtMs": 5000,
  "isFinal": true
}
```

### 요청 필드

- `chunkId`: STT 청크 고유 ID
- `sequence`: 세션 내 순번, `1`부터 시작
- `text`: 정제된 STT 텍스트
- `startedAtMs`: 통화 시작 기준 청크 시작 시점
- `endedAtMs`: 통화 시작 기준 청크 종료 시점
- `isFinal`: 현재는 `true`만 허용

### 응답 예시

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "chunkId": "chunk-001",
  "acceptedSequence": 1,
  "nextTranscriptSequence": 2,
  "duplicate": false,
  "analysisThresholdReached": true,
  "riskScore": 82,
  "phishingType": "ACCOUNT_TRANSFER_INDUCEMENT",
  "aiSummary": "계좌 이체 유도 표현이 반복적으로 탐지되었습니다.",
  "keywords": [
    "안전계좌",
    "이체",
    "검찰"
  ],
  "shouldOpenWebSocket": true
}
```

### 응답 필드

- `acceptedSequence`: 서버가 정상 수락한 sequence
- `nextTranscriptSequence`: 다음 요청에서 사용해야 하는 sequence
- `duplicate`: 이미 처리된 중복 청크인지 여부
- `analysisThresholdReached`: 분석 수행 기준을 넘겼는지 여부
- `riskScore`: 0~100 위험 점수
- `phishingType`: 판단된 피싱 유형
- `aiSummary`: FastAPI 분석 요약
- `keywords`: 탐지 키워드 목록
- `shouldOpenWebSocket`: WebSocket 실시간 감시 단계로 전환해야 하는지 여부

### Flutter 처리 규칙

- `shouldOpenWebSocket == false`
- 계속 REST 분석 API를 호출한다.

- `shouldOpenWebSocket == true`
- WebSocket 연결을 시작한다.
- 이후 STT 전송은 WebSocket 기반으로 전환한다.

## 8. 2단계: WebSocket 실시간 감시

### 연결 주소

`ws://{backend-host}:8080/ws/v1/victim?sessionId={sessionId}`

HTTPS 환경에서는 `wss://` 를 사용한다.

### 연결 직후 서버 이벤트

#### `SESSION_READY`

```json
{
  "eventId": "server-event-001",
  "eventType": "SESSION_READY",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "occurredAt": "2026-06-10T15:21:00+09:00",
  "data": {
    "nextTranscriptSequence": 5
  }
}
```

Flutter는 이 값을 기준으로 다음 sequence를 동기화해야 한다.

## 9. Flutter -> Backend WebSocket 이벤트

### 9-1. `TRANSCRIPT_CHUNK`

```json
{
  "eventId": "event-001",
  "eventType": "TRANSCRIPT_CHUNK",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "occurredAt": "2026-06-10T15:21:05+09:00",
  "data": {
    "chunkId": "chunk-005",
    "sequence": 5,
    "text": "검찰입니다. 지금 앱을 설치하고 인증번호를 입력하세요.",
    "startedAtMs": 20000,
    "endedAtMs": 25000,
    "isFinal": true
  }
}
```

### 9-2. `SESSION_COMPLETE`

```json
{
  "eventId": "event-999",
  "eventType": "SESSION_COMPLETE",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "occurredAt": "2026-06-10T15:32:00+09:00",
  "data": {
    "endedAt": "2026-06-10T15:32:00+09:00",
    "lastTranscriptSequence": 42
  }
}
```

### 9-3. `PING`

```json
{
  "eventId": "event-ping-001",
  "eventType": "PING",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "occurredAt": "2026-06-10T15:21:10+09:00",
  "data": {}
}
```

## 10. Backend -> Flutter WebSocket 이벤트

### 10-1. `TRANSCRIPT_ACK`

```json
{
  "eventId": "server-event-010",
  "eventType": "TRANSCRIPT_ACK",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "occurredAt": "2026-06-10T15:21:05+09:00",
  "data": {
    "chunkId": "chunk-005",
    "acceptedSequence": 5,
    "nextTranscriptSequence": 6,
    "duplicate": false,
    "analysisThresholdReached": true
  }
}
```

### 10-2. `ANALYSIS_RESULT`

```json
{
  "eventId": "server-event-011",
  "eventType": "ANALYSIS_RESULT",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "occurredAt": "2026-06-10T15:21:05+09:00",
  "data": {
    "chunkId": "chunk-005",
    "sequence": 5,
    "riskScore": 91,
    "phishingType": "REMOTE_APP_INSTALLATION",
    "aiSummary": "원격 앱 설치와 인증번호 입력 유도가 반복 탐지되었습니다.",
    "keywords": [
      "앱 설치",
      "인증번호",
      "보안"
    ],
    "provider": "fastapi"
  }
}
```

### 10-3. `ANALYSIS_ERROR`

```json
{
  "eventId": "server-event-012",
  "eventType": "ANALYSIS_ERROR",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "occurredAt": "2026-06-10T15:21:05+09:00",
  "data": {
    "chunkId": "chunk-005",
    "sequence": 5,
    "reason": "INVALID_REQUEST",
    "message": "FastAPI 분석 요청 처리에 실패했습니다.",
    "details": {
      "channel": "websocket",
      "message": "..."
    }
  }
}
```

### 10-4. `TRANSCRIPT_NACK`

아래 상황에서 반환될 수 있다.

- sequence 불일치
- 잘못된 payload
- 세션 상태 불일치

### 10-5. `SESSION_COMPLETE_ACK`

```json
{
  "eventId": "server-event-999",
  "eventType": "SESSION_COMPLETE_ACK",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "occurredAt": "2026-06-10T15:32:00+09:00",
  "data": {
    "status": "COMPLETED",
    "finalAnalysisQueued": true
  }
}
```

### 10-6. `PONG`

```json
{
  "eventId": "server-event-pong-001",
  "eventType": "PONG",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "occurredAt": "2026-06-10T15:21:10+09:00",
  "data": {}
}
```

## 11. REST 에러 응답 규격

REST 에러는 공통적으로 JSON 구조를 사용한다.

```json
{
  "code": "INVALID_REQUEST",
  "message": "STT 텍스트는 비어 있을 수 없습니다.",
  "details": {
    "expectedSequence": 3
  }
}
```

## 12. Flutter 구현 체크리스트

1. 통화 시작 시 세션 생성 API를 먼저 호출한다.
2. `userId`는 MVP 단계에서 더미값 `1001`을 사용한다.
3. `nextTranscriptSequence`를 기준으로 sequence를 관리한다.
4. 초기 5초 STT는 REST 분석 API로 전송한다.
5. REST 응답에서 `shouldOpenWebSocket=true`가 오면 WebSocket으로 전환한다.
6. WebSocket 연결 직후 `SESSION_READY.nextTranscriptSequence`로 sequence를 다시 맞춘다.
7. 이후 STT는 `TRANSCRIPT_CHUNK` 이벤트로 전송한다.
8. `ANALYSIS_RESULT` 수신 시 UI의 위험도, 요약, 키워드를 갱신한다.
9. 통화 종료 시 `SESSION_COMPLETE` 이벤트를 전송한다.
