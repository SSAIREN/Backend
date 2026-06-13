package com.ssairen.backend.domain.callsession.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssairen.backend.domain.callsession.analysis.TranscriptAnalysisService;
import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisResult;
import com.ssairen.backend.domain.callsession.api.dto.CallSessionResponse;
import com.ssairen.backend.domain.callsession.api.dto.SessionCompletionResult;
import com.ssairen.backend.domain.callsession.application.CallSessionService;
import com.ssairen.backend.domain.callsession.websocket.dto.SessionCompletePayload;
import com.ssairen.backend.domain.callsession.websocket.dto.TranscriptAcceptResult;
import com.ssairen.backend.domain.callsession.websocket.dto.TranscriptChunkPayload;
import com.ssairen.backend.domain.callsession.websocket.dto.VictimClientEvent;
import com.ssairen.backend.domain.callsession.websocket.dto.VictimServerEvent;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class VictimWebSocketHandler extends TextWebSocketHandler {

    /*
     * 연결된 WebSocket과 통화 세션을 바인딩하기 위해
     * handshake 직후 session attribute에 callSessionId를 저장한다.
     */
    private static final String SESSION_ID_ATTRIBUTE = "callSessionId";

    private final ObjectMapper objectMapper;
    private final CallSessionService callSessionService;
    private final TranscriptAnalysisService transcriptAnalysisService;

    public VictimWebSocketHandler(
            ObjectMapper objectMapper,
            CallSessionService callSessionService,
            TranscriptAnalysisService transcriptAnalysisService
    ) {
        this.objectMapper = objectMapper;
        this.callSessionService = callSessionService;
        this.transcriptAnalysisService = transcriptAnalysisService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String callSessionId = extractSessionId(session);
        CallSessionResponse callSession = callSessionService.getSession(callSessionId);
        session.getAttributes().put(SESSION_ID_ATTRIBUTE, callSessionId);

        log.debug(
                "Flutter WebSocket connected. sessionId={}, socketId={}, remoteAddress={}",
                callSessionId,
                session.getId(),
                session.getRemoteAddress()
        );

        /*
         * 프론트는 이 응답을 기준으로 다음에 보내야 할 sequence를 맞춰야 한다.
         * 재연결 상황에서도 서버가 기대하는 sequence를 다시 전달하기 위해
         * 연결 직후 SESSION_READY를 항상 내려준다.
         */
        sendEvent(session, VictimServerEvent.of(
                "SESSION_READY",
                callSessionId,
                Map.of("nextTranscriptSequence", callSession.nextTranscriptSequence())
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String callSessionId = getBoundSessionId(session);

        try {
            log.debug(
                    "Received Flutter WebSocket message. sessionId={}, socketId={}, payload={}",
                    callSessionId,
                    session.getId(),
                    message.getPayload()
            );

            VictimClientEvent event = objectMapper.readValue(message.getPayload(), VictimClientEvent.class);
            validateEnvelope(event, callSessionId);

            switch (event.eventType()) {
                case "TRANSCRIPT_CHUNK" -> handleTranscriptChunk(session, event);
                case "SESSION_COMPLETE" -> handleSessionComplete(session, event);
                case "PING" -> sendEvent(session, VictimServerEvent.of("PONG", callSessionId, Map.of()));
                default -> throw new BusinessException(
                        ErrorCode.INVALID_REQUEST,
                        "지원하지 않는 WebSocket 이벤트입니다.",
                        Map.of("eventType", event.eventType())
                );
            }
        } catch (BusinessException exception) {
            sendNack(session, callSessionId, exception);
        } catch (JsonProcessingException exception) {
            sendNack(session, callSessionId, new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "WebSocket JSON 메시지 형식이 올바르지 않습니다."
            ));
        }
    }

    private void handleTranscriptChunk(WebSocketSession session, VictimClientEvent event) throws IOException {
        TranscriptChunkPayload payload = objectMapper.treeToValue(event.data(), TranscriptChunkPayload.class);
        validateTranscriptPayload(payload);

        /*
         * transcript 저장과 sequence 검증은 세션 서비스에서 수행한다.
         * 성공하면 먼저 ACK를 반환하고, 그 다음 FastAPI 분석 결과를 내려준다.
         */
        TranscriptAcceptResult result = callSessionService.acceptTranscript(
                event.sessionId(),
                payload.chunkId(),
                payload.sequence(),
                payload.text(),
                payload.startedAtMs(),
                payload.endedAtMs(),
                payload.isFinal()
        );

        sendEvent(session, VictimServerEvent.of(
                "TRANSCRIPT_ACK",
                event.sessionId(),
                Map.of(
                        "chunkId", result.chunkId(),
                        "acceptedSequence", result.acceptedSequence(),
                        "nextTranscriptSequence", result.nextSequence(),
                        "duplicate", result.duplicate(),
                        "analysisThresholdReached", result.analysisThresholdReached()
                )
        ));

        try {
            TranscriptAnalysisResult analysisResult = transcriptAnalysisService.analyzeWebSocketChunk(
                    event.sessionId(),
                    payload.sequence(),
                    payload.text()
            );

            sendEvent(session, VictimServerEvent.of(
                    "ANALYSIS_RESULT",
                    event.sessionId(),
                    Map.of(
                            "chunkId", payload.chunkId(),
                            "sequence", payload.sequence(),
                            "riskScore", analysisResult.riskScore(),
                            "phishingType", analysisResult.phishingType(),
                            "aiSummary", analysisResult.aiSummary(),
                            "keywords", analysisResult.keywords(),
                            "provider", analysisResult.provider()
                    )
            ));
        } catch (BusinessException exception) {
            sendEvent(session, VictimServerEvent.of(
                    "ANALYSIS_ERROR",
                    event.sessionId(),
                    Map.of(
                            "chunkId", payload.chunkId(),
                            "sequence", payload.sequence(),
                            "reason", exception.getErrorCode().name(),
                            "message", exception.getMessage(),
                            "details", exception.getDetails()
                    )
            ));
        }
    }

    private void handleSessionComplete(WebSocketSession session, VictimClientEvent event) throws IOException {
        SessionCompletePayload payload = objectMapper.treeToValue(event.data(), SessionCompletePayload.class);
        if (payload.endedAt() == null || payload.lastTranscriptSequence() < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "통화 종료 데이터가 올바르지 않습니다.");
        }

        SessionCompletionResult result = callSessionService.completeSession(
                event.sessionId(),
                payload.endedAt(),
                payload.lastTranscriptSequence()
        );

        sendEvent(session, VictimServerEvent.of(
                "SESSION_COMPLETE_ACK",
                event.sessionId(),
                Map.of(
                        "status", result.response().status(),
                        "finalAnalysisQueued", result.finalAnalysisQueued()
                )
        ));
    }

    private void validateEnvelope(VictimClientEvent event, String boundSessionId) {
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "eventId는 필수입니다.");
        }
        if (event.eventType() == null || event.eventType().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "eventType은 필수입니다.");
        }
        if (!boundSessionId.equals(event.sessionId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "연결된 통화 세션과 메시지의 sessionId가 다릅니다.");
        }
        if (event.data() == null || event.data().isNull()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "data는 필수입니다.");
        }
    }

    private void validateTranscriptPayload(TranscriptChunkPayload payload) {
        if (payload.chunkId() == null || payload.chunkId().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "chunkId는 필수입니다.");
        }
        if (payload.sequence() < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "sequence는 1 이상이어야 합니다.");
        }
        if (!payload.isFinal()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "현재 서버는 확정된 STT 청크만 수신합니다.");
        }
    }

    private void sendNack(WebSocketSession session, String callSessionId, BusinessException exception) throws IOException {
        sendEvent(session, VictimServerEvent.of(
                "TRANSCRIPT_NACK",
                callSessionId,
                Map.of(
                        "reason", exception.getErrorCode().name(),
                        "message", exception.getMessage(),
                        "details", exception.getDetails()
                )
        ));
    }

    private String extractSessionId(WebSocketSession session) {
        String sessionId = UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "WebSocket 연결에는 sessionId가 필요합니다.");
        }
        return sessionId;
    }

    private String getBoundSessionId(WebSocketSession session) {
        Object sessionId = session.getAttributes().get(SESSION_ID_ATTRIBUTE);
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.CALL_SESSION_NOT_FOUND, "WebSocket 연결에 바인딩된 통화 세션이 없습니다.");
        }
        return sessionId.toString();
    }

    private void sendEvent(WebSocketSession session, VictimServerEvent event) throws IOException {
        synchronized (session) {
            if (session.isOpen()) {
                String payload = objectMapper.writeValueAsString(event);
                log.debug(
                        "Sending WebSocket message to Flutter. sessionId={}, socketId={}, eventType={}, payload={}",
                        event.sessionId(),
                        session.getId(),
                        event.eventType(),
                        payload
                );
                session.sendMessage(new TextMessage(payload));
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.debug(
                "Flutter WebSocket transport error. sessionId={}, socketId={}, message={}",
                session.getAttributes().get(SESSION_ID_ATTRIBUTE),
                session.getId(),
                exception.getMessage(),
                exception
        );
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.debug(
                "Flutter WebSocket closed. sessionId={}, socketId={}, closeCode={}, reason={}",
                session.getAttributes().get(SESSION_ID_ATTRIBUTE),
                session.getId(),
                status.getCode(),
                status.getReason()
        );
        super.afterConnectionClosed(session, status);
    }
}
