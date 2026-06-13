# Backend - FastAPI API Specification

## 1. 개요

이 문서는 Spring Boot Backend가 FastAPI AI 서버로 전달하는 분석 요청과,
FastAPI가 Spring Boot로 반환해야 하는 응답 형식을 정의한다.

현재 분석 경로는 2개다.

1. REST 초기 분석 경로
- Flutter가 통화 시작 직후 5초 단위 STT를 Spring REST API로 보내는 단계
- Spring은 이를 FastAPI의 REST 분석 endpoint로 전달한다

2. WebSocket 실시간 분석 경로
- REST 분석 결과 `shouldOpenWebSocket=true`가 내려온 이후 단계
- Flutter는 Spring WebSocket으로 STT를 보내고, Spring은 이를 FastAPI의 실시간 분석 endpoint로 전달한다

두 endpoint 모두 기본 분석 payload는 유사하지만, 호출 맥락이 다르므로 path를 분리한다.

## 2. Base 정보

- Base URL: `http://{fastapi-host}:8000`
- Content-Type: `application/json`

현재 Backend 설정 키:

- `ssairen.analysis.fastapi.url`
- `ssairen.analysis.fastapi.rest-path`
- `ssairen.analysis.fastapi.websocket-path`

기본값:

- REST 분석 path: `/api/v1/call-analysis/rest`
- WebSocket 분석 path: `/api/v1/call-analysis/websocket`

## 3. REST 초기 분석 endpoint

### `POST /api/v1/call-analysis/rest`

통화 시작 직후, Flutter가 5초 단위 STT를 REST로 보내면 Spring이 FastAPI에 전달하는 endpoint다.

### 요청 예시

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "chunkId": "chunk-001",
  "sequence": 1,
  "transcript": "검찰 수사관입니다. 안전계좌로 이체하세요.",
  "startedAtMs": 0,
  "endedAtMs": 5000,
  "isFinal": true,
  "channel": "rest",
  "userId": 1001,
  "victimName": "김영희",
  "victimAge": 71,
  "victimPhone": "01012345678"
}
```

### 필드 설명

- `sessionId`: Spring Backend가 발급한 통화 세션 ID
- `chunkId`: 이번 STT 청크의 고유 ID
- `sequence`: 세션 내 STT 순번
- `transcript`: 이번 5초 구간의 정제된 STT 텍스트
- `startedAtMs`: 통화 시작 기준 청크 시작 시점 밀리초
- `endedAtMs`: 통화 시작 기준 청크 종료 시점 밀리초
- `isFinal`: 확정된 STT 청크 여부
- `channel`: 호출 경로 구분값, REST 초기 분석은 항상 `rest`
- `userId`: MVP 단계에서 Flutter가 전달하는 사용자 식별값
- `victimName`: 피해자 이름
- `victimAge`: 피해자 나이
- `victimPhone`: 통화 중인 피해자 전화번호 또는 대표 식별 전화번호

## 4. WebSocket 실시간 분석 endpoint

### `POST /api/v1/call-analysis/websocket`

Flutter가 Spring WebSocket `/ws/v1/victim`으로 STT를 보내는 실시간 단계에서,
Spring이 FastAPI에 REST로 재전달하는 endpoint다.

### 요청 예시

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "chunkId": "chunk-008",
  "sequence": 8,
  "transcript": "검찰입니다. 지금 앱을 설치하고 인증번호를 입력하세요.",
  "startedAtMs": 35000,
  "endedAtMs": 40000,
  "isFinal": true,
  "channel": "websocket",
  "userId": 1001,
  "victimName": "김영희",
  "victimAge": 71,
  "victimPhone": "01012345678"
}
```

### 설명

- WebSocket 단계에서도 Spring -> FastAPI 호출은 HTTP REST로 수행한다.
- 다만 호출 맥락이 실시간 고위험 감시 단계이므로 endpoint를 분리한다.
- FastAPI는 이 경로에서 추가 메타데이터나 세밀한 위험 분석을 수행할 수 있다.

## 5. FastAPI 응답 규격

Backend가 현재 기대하는 FastAPI 응답 필드는 아래와 같다.

### 응답 예시

```json
{
  "riskScore": 91,
  "phishingType": "REMOTE_APP_INSTALLATION",
  "aiSummary": "원격 앱 설치와 인증번호 입력 유도가 반복적으로 탐지되었습니다.",
  "keywords": [
    "앱 설치",
    "인증번호",
    "원격 제어"
  ]
}
```

### 필드 설명

- `riskScore`
- 정수
- 범위: `0 ~ 100`

- `phishingType`
- 문자열 enum
- Backend 사용값:
- `AGENCY_IMPERSONATION`
- `ACCOUNT_TRANSFER_INDUCEMENT`
- `KIDNAPPING_THREAT`
- `REMOTE_APP_INSTALLATION`
- `UNKNOWN`

- `aiSummary`
- 문자열
- FastAPI 모델이 생성한 요약 문장

- `keywords`
- 문자열 배열
- 탐지 키워드 목록

## 6. Backend 내부 처리 방식

FastAPI 응답을 받으면 Backend는 다음 동작을 수행한다.

1. `riskScore`를 `0~100` 범위로 보정한다.
2. `phishingType`를 Backend enum으로 변환한다.
3. `FraudCase` 엔티티에 최신 분석 결과를 반영한다.
4. REST 단계라면 Flutter REST 응답으로 결과를 반환한다.
5. WebSocket 단계라면 Flutter에 `ANALYSIS_RESULT` 이벤트로 전달한다.
6. 위험도가 임계치를 넘으면 현재 `userId`와 연결된 보호자에게 FCM 알림 전송을 시도한다.

## 7. FastAPI 구현 시 주의사항

1. 응답 본문은 반드시 JSON 이어야 한다.
2. 필드명은 아래 casing을 그대로 사용한다.
- `riskScore`
- `phishingType`
- `aiSummary`
- `keywords`
3. `phishingType`가 비어 있거나 알 수 없는 값이면 Backend에서 `UNKNOWN` 또는 null 처리될 수 있다.
4. timeout, validation 실패, 모델 추론 실패 같은 오류는 HTTP 에러와 JSON 메시지로 반환하는 것을 권장한다.

## 8. 권장 에러 응답 예시

Backend는 FastAPI 호출 실패 시 메시지를 감싸서 REST 에러 또는 WebSocket `ANALYSIS_ERROR` 이벤트로 전달한다.
FastAPI도 아래와 같은 구조의 JSON 에러를 주는 것을 권장한다.

```json
{
  "code": "MODEL_INFERENCE_FAILED",
  "message": "모델 추론 중 오류가 발생했습니다."
}
```

## 9. channel 값 의미

- `rest`
- 통화 초반의 초기 분석 단계
- Flutter REST 응답 기반으로 UI를 갱신하는 단계

- `websocket`
- 위험도 상승 후 실시간 감시 단계
- Flutter가 Spring WebSocket에 연결된 이후 사용
- WebSocket 이벤트 기반으로 UI를 계속 갱신하는 단계

## 10. 관련 Backend 코드 참고

관련 파일:

- `src/main/java/com/ssairen/backend/domain/callsession/analysis/FastApiTranscriptAnalysisGateway.java`
- `src/main/java/com/ssairen/backend/domain/callsession/analysis/TranscriptAnalysisService.java`
- `src/main/java/com/ssairen/backend/global/config/SwaggerConfig.java`

현재 Backend는 FastAPI 전용 구조이며, OpenAI fallback 호출은 존재하지 않는다.
