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
                        "Unsupported WebSocket event type.",
                        Map.of("eventType", event.eventType())
                );
            }
        } catch (BusinessException exception) {
            sendNack(session, callSessionId, exception);
        } catch (JsonProcessingException exception) {
            sendNack(session, callSessionId, new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "WebSocket JSON message format is invalid."
            ));
        }
    }

    private void handleTranscriptChunk(WebSocketSession session, VictimClientEvent event) throws IOException {
        TranscriptChunkPayload payload = objectMapper.treeToValue(event.data(), TranscriptChunkPayload.class);
        validateTranscriptPayload(payload);

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
                    payload.chunkId(),
                    payload.sequence()
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
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Session completion payload is invalid.");
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
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "eventId is required.");
        }
        if (event.eventType() == null || event.eventType().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "eventType is required.");
        }
        if (!boundSessionId.equals(event.sessionId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Message sessionId does not match the bound call session.");
        }
        if (event.data() == null || event.data().isNull()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "data is required.");
        }
    }

    private void validateTranscriptPayload(TranscriptChunkPayload payload) {
        if (payload.chunkId() == null || payload.chunkId().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "chunkId is required.");
        }
        if (payload.sequence() < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "sequence must be at least 1.");
        }
        if (!payload.isFinal()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "The current server only accepts final STT chunks.");
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
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "WebSocket connection requires a sessionId.");
        }
        return sessionId;
    }

    private String getBoundSessionId(WebSocketSession session) {
        Object sessionId = session.getAttributes().get(SESSION_ID_ATTRIBUTE);
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.CALL_SESSION_NOT_FOUND, "No call session is bound to this WebSocket connection.");
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
