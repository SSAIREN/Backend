# Flutter WebSocket Integration Guide

## 1. 문서 목적

이 문서는 현재 `Backend` 코드 기준으로 Flutter가 소켓을 어떻게 연결하고,
각 이벤트에서 어떤 URL로 어떤 파라미터를 보내야 하며,
응답은 어떤 형태로 받아야 하는지 상세하게 설명한다.

대상 서버:

- REST 세션 생성: `/api/mobile/call-sessions`
- REST 초기 분석: `/api/mobile/call-sessions/{sessionId}/transcripts/analyze`
- WebSocket 연결: `/ws/v1/victim`

## 2. 전체 흐름 요약

현재 구현 기준으로 Flutter는 아래 순서로 동작해야 한다.

1. REST로 세션 생성
2. REST로 5초 단위 STT 초기 분석
3. REST 응답에서 `shouldOpenWebSocket == true`가 오면 WebSocket 연결
4. WebSocket으로 `TRANSCRIPT_CHUNK` 전송
5. 서버로부터 `TRANSCRIPT_ACK`, `ANALYSIS_RESULT`, `ANALYSIS_ERROR`, `TRANSCRIPT_NACK` 수신
6. 통화 종료 시 `SESSION_COMPLETE` 전송
7. 서버로부터 `SESSION_COMPLETE_ACK` 수신 후 종료 처리

## 3. WebSocket 연결 명세

### 연결 URL

개발 환경:

```text
ws://{backend-host}:8080/ws/v1/victim?sessionId={sessionId}
```

예시:

```text
ws://localhost:8080/ws/v1/victim?sessionId=550e8400-e29b-41d4-a716-446655440000
```

### URL Query Parameter

- `sessionId`
- 타입: `String`
- 필수 여부: 필수
- 설명: REST 세션 생성 API에서 받은 통화 세션 ID

### 연결 직후 서버 응답

서버는 소켓 연결 직후 아래 이벤트를 먼저 보낸다.

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

Flutter는 반드시 `data.nextTranscriptSequence` 값을 저장하고, 이후 청크 전송 시 이 값을 기준으로 `sequence`를 맞춰야 한다.

## 4. 공통 이벤트 구조

모든 WebSocket 메시지는 아래 envelope 구조를 사용한다.

```json
{
  "eventId": "string",
  "eventType": "string",
  "sessionId": "string",
  "occurredAt": "ISO-8601 datetime with offset",
  "data": {}
}
```

### 공통 필드 설명

- `eventId`
  - 타입: `String`
  - 필수
  - Flutter가 생성하는 이벤트 고유 ID

- `eventType`
  - 타입: `String`
  - 필수
  - 이벤트 종류

- `sessionId`
  - 타입: `String`
  - 필수
  - URL의 `sessionId`와 반드시 같아야 함

- `occurredAt`
  - 타입: `String`
  - 필수
  - ISO-8601 형식의 시간 문자열

- `data`
  - 타입: `Object`
  - 필수
  - 이벤트별 실제 payload

## 5. Flutter -> Backend 이벤트 명세

## 5-1. `TRANSCRIPT_CHUNK`

### 언제 보내나

WebSocket 단계에서 5초 단위 STT 텍스트를 서버로 보낼 때 사용한다.

### 전송 대상 URL

이미 연결된 WebSocket:

```text
/ws/v1/victim?sessionId={sessionId}
```

별도의 추가 path는 없다. 기존 소켓 연결 위로 메시지만 보내면 된다.

### 보내야 하는 envelope

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

### `data` 파라미터

- `chunkId`
  - 타입: `String`
  - 필수
  - STT 청크 고유 ID

- `sequence`
  - 타입: `number`
  - 필수
  - 현재 서버가 기대하는 STT 순번
  - 반드시 `SESSION_READY.nextTranscriptSequence` 또는 직전 `TRANSCRIPT_ACK.nextTranscriptSequence` 값을 사용

- `text`
  - 타입: `String`
  - 필수
  - 5초 단위 STT 텍스트

- `startedAtMs`
  - 타입: `number`
  - 필수
  - 통화 시작 기준 청크 시작 밀리초

- `endedAtMs`
  - 타입: `number`
  - 필수
  - 통화 시작 기준 청크 종료 밀리초
  - `startedAtMs` 이상이어야 함

- `isFinal`
  - 타입: `boolean`
  - 필수
  - 현재 서버는 `true`만 허용

### 서버 응답 1: `TRANSCRIPT_ACK`

청크 저장과 sequence 검증이 성공하면 먼저 ACK가 온다.

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

#### `TRANSCRIPT_ACK.data`

- `chunkId`
  - 타입: `String`
    - 설명: 서버가 처리한 청크 ID

- `acceptedSequence`
  - 타입: `number`
  - 설명: 서버가 정상 수락한 sequence

- `nextTranscriptSequence`
  - 타입: `number`
  - 설명: 다음에 Flutter가 보내야 하는 sequence

- `duplicate`
  - 타입: `boolean`
  - 설명: 이미 저장된 동일 청크의 재전송인지 여부

- `analysisThresholdReached`
  - 타입: `boolean`
  - 설명: 누적 transcript 길이가 분석 기준을 넘었는지 여부

#### Flutter 처리

1. `nextTranscriptSequence`를 로컬 상태에 저장
2. 다음 STT 청크 전송 시 이 값을 사용
3. `duplicate` 여부는 표시용으로만 참고하고, sequence는 서버 값을 그대로 따름

### 서버 응답 2: `ANALYSIS_RESULT`

FastAPI 분석이 성공하면 ACK 이후 별도로 결과가 온다.

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

#### `ANALYSIS_RESULT.data`

- `chunkId`
  - 타입: `String`

- `sequence`
  - 타입: `number`

- `riskScore`
  - 타입: `number`
  - 범위: `0 ~ 100`

- `phishingType`
  - 타입: `String`
  - 예시: `REMOTE_APP_INSTALLATION`

- `aiSummary`
  - 타입: `String`

- `keywords`
  - 타입: `String[]`

- `provider`
  - 타입: `String`
  - 현재 값: `fastapi`

#### Flutter 처리

1. 위험도 UI 갱신
2. 요약 문장 갱신
3. 키워드 목록 갱신
4. 필요 시 경고 상태 표시

### 서버 응답 3: `ANALYSIS_ERROR`

청크는 수락됐지만 FastAPI 분석이 실패하면 온다.

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

#### Flutter 처리

1. sequence는 되돌리지 않음
2. 현재 청크는 서버가 이미 수락했을 수 있으므로 재전송 기준으로 삼지 않음
3. UI에 분석 실패 상태만 보여줌

### 서버 응답 4: `TRANSCRIPT_NACK`

입력 자체가 잘못되었거나 sequence가 틀리면 온다.

```json
{
  "eventId": "server-event-013",
  "eventType": "TRANSCRIPT_NACK",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "occurredAt": "2026-06-10T15:21:05+09:00",
  "data": {
    "reason": "TRANSCRIPT_SEQUENCE_MISMATCH",
    "message": "STT 청크 sequence가 올바르지 않습니다.",
    "details": {
      "expectedSequence": 6
    }
  }
}
```

#### Flutter 처리

1. `details.expectedSequence`가 있으면 로컬 `nextTranscriptSequence`를 그 값으로 수정
2. 현재 청크를 재전송할지 폐기할지 결정
3. 잘못된 `sessionId`, `isFinal=false`, 빈 `text`, 잘못된 sequence`인지 로그로 남김

## 5-2. `SESSION_COMPLETE`

### 언제 보내나

통화 종료 시 보낸다.

### 전송 대상 URL

이미 연결된 WebSocket:

```text
/ws/v1/victim?sessionId={sessionId}
```

### 보내야 하는 envelope

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

### `data` 파라미터

- `endedAt`
  - 타입: `String`
  - 필수
  - ISO-8601 시간 문자열

- `lastTranscriptSequence`
  - 타입: `number`
  - 필수
  - 마지막으로 서버가 정상 수락한 sequence
  - 직전 `TRANSCRIPT_ACK.acceptedSequence` 값을 넣는 것이 가장 안전함

### 서버 응답: `SESSION_COMPLETE_ACK`

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

#### `SESSION_COMPLETE_ACK.data`

- `status`
  - 타입: `String`
  - 예시: `COMPLETED`

- `finalAnalysisQueued`
  - 타입: `boolean`
  - 설명: 마지막 후처리 분석이 예약되었는지 여부

#### Flutter 처리

1. STT 전송 중단
2. 세션 UI 종료 상태 반영
3. 필요 시 WebSocket 종료

## 5-3. `PING`

### 언제 보내나

헬스체크가 필요할 때 보낸다.

### 전송 대상 URL

이미 연결된 WebSocket:

```text
/ws/v1/victim?sessionId={sessionId}
```

### 보내야 하는 envelope

```json
{
  "eventId": "event-ping-001",
  "eventType": "PING",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "occurredAt": "2026-06-10T15:21:10+09:00",
  "data": {}
}
```

### 서버 응답: `PONG`

```json
{
  "eventId": "server-event-pong-001",
  "eventType": "PONG",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "occurredAt": "2026-06-10T15:21:10+09:00",
  "data": {}
}
```

#### Flutter 처리

- 소켓 연결이 살아 있다고 판단
- 필요 시 마지막 헬스체크 시간 갱신

## 6. Flutter 구현 시 필수 로컬 상태

- `sessionId`
- `socketConnected`
- `nextTranscriptSequence`
- `lastAcceptedSequence`
- `lastRiskScore`
- `lastAiSummary`
- `lastKeywords`

## 7. 재연결 시 처리 방법

소켓이 끊어졌다면 아래 순서를 권장한다.

1. `GET /api/mobile/call-sessions/{sessionId}` 로 세션 상태 조회
2. WebSocket 재연결
3. `SESSION_READY.nextTranscriptSequence` 수신
4. 로컬 sequence를 이 값으로 다시 맞춤
5. 아직 서버에 반영되지 않은 청크만 다시 전송

중요:

- 재연결 후에는 Flutter 메모리의 기존 sequence보다 서버가 준 `SESSION_READY` 값을 우선해야 한다.

## 8. Flutter 예시 코드

### 소켓 연결

```dart
final uri = Uri.parse(
  'ws://localhost:8080/ws/v1/victim?sessionId=$sessionId',
);

final channel = WebSocketChannel.connect(uri);
```

### 메시지 수신 처리

```dart
channel.stream.listen((raw) {
  final event = jsonDecode(raw as String);

  switch (event['eventType']) {
    case 'SESSION_READY':
      nextTranscriptSequence = event['data']['nextTranscriptSequence'];
      break;

    case 'TRANSCRIPT_ACK':
      lastAcceptedSequence = event['data']['acceptedSequence'];
      nextTranscriptSequence = event['data']['nextTranscriptSequence'];
      break;

    case 'ANALYSIS_RESULT':
      riskScore = event['data']['riskScore'];
      aiSummary = event['data']['aiSummary'];
      keywords = List<String>.from(event['data']['keywords'] ?? []);
      break;

    case 'TRANSCRIPT_NACK':
      final expected = event['data']['details']?['expectedSequence'];
      if (expected != null) {
        nextTranscriptSequence = expected;
      }
      break;

    case 'ANALYSIS_ERROR':
      break;

    case 'SESSION_COMPLETE_ACK':
      channel.sink.close();
      break;

    case 'PONG':
      break;
  }
});
```

### `TRANSCRIPT_CHUNK` 전송

```dart
channel.sink.add(jsonEncode({
  'eventId': const Uuid().v4(),
  'eventType': 'TRANSCRIPT_CHUNK',
  'sessionId': sessionId,
  'occurredAt': DateTime.now().toIso8601String(),
  'data': {
    'chunkId': const Uuid().v4(),
    'sequence': nextTranscriptSequence,
    'text': sttText,
    'startedAtMs': startedAtMs,
    'endedAtMs': endedAtMs,
    'isFinal': true,
  }
}));
```

### `SESSION_COMPLETE` 전송

```dart
channel.sink.add(jsonEncode({
  'eventId': const Uuid().v4(),
  'eventType': 'SESSION_COMPLETE',
  'sessionId': sessionId,
  'occurredAt': DateTime.now().toIso8601String(),
  'data': {
    'endedAt': DateTime.now().toIso8601String(),
    'lastTranscriptSequence': lastAcceptedSequence,
  }
}));
```

### `PING` 전송

```dart
channel.sink.add(jsonEncode({
  'eventId': const Uuid().v4(),
  'eventType': 'PING',
  'sessionId': sessionId,
  'occurredAt': DateTime.now().toIso8601String(),
  'data': {}
}));
```

## 9. 가장 중요한 규칙

1. WebSocket URL의 `sessionId`와 메시지 body의 `sessionId`는 반드시 같아야 한다.
2. `sequence`는 Flutter가 임의 증가시키지 말고 서버 응답값을 기준으로 관리해야 한다.
3. `TRANSCRIPT_ACK`는 입력 수락 응답이고, `ANALYSIS_RESULT`는 분석 결과 응답이다. 둘은 별개로 처리해야 한다.
4. `TRANSCRIPT_NACK`는 입력 거절이고, `ANALYSIS_ERROR`는 분석 실패다. 처리 방식이 다르다.
5. `SESSION_COMPLETE_ACK`를 받아야 서버 종료 처리까지 완료된 것으로 보는 것이 안전하다.
