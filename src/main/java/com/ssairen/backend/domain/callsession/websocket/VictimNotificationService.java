package com.ssairen.backend.domain.callsession.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssairen.backend.domain.callsession.websocket.dto.VictimServerEvent;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Service
public class VictimNotificationService {

    private final VictimSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public VictimNotificationService(VictimSessionRegistry sessionRegistry, ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    public void pushEvent(String callSessionId, String eventType, Object data) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(VictimServerEvent.of(eventType, callSessionId, data));
        } catch (JsonProcessingException exception) {
            log.error("피해자 WebSocket payload 직렬화에 실패했습니다. sessionId={}, eventType={}", callSessionId, eventType, exception);
            return;
        }

        for (WebSocketSession session : sessionRegistry.getSessions(callSessionId)) {
            sendMessage(session, payload);
        }
    }

    private void sendMessage(WebSocketSession session, String payload) {
        synchronized (session) {
            if (!session.isOpen()) {
                return;
            }
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException exception) {
                log.warn("피해자 WebSocket 메시지 전송에 실패했습니다. socketId={}", session.getId(), exception);
            }
        }
    }
}
