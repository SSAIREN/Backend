package com.ssairen.backend.domain.casefile.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssairen.backend.domain.casefile.dto.CaseSummaryResponse;
import com.ssairen.backend.domain.casefile.entity.FraudCase;
import com.ssairen.backend.domain.casefile.websocket.DashboardSessionRegistry;
import com.ssairen.backend.domain.casefile.websocket.dto.ActionUpdateEvent;
import com.ssairen.backend.domain.casefile.websocket.dto.NewCaseEvent;
import com.ssairen.backend.domain.responseaction.entity.ResponseActionStatus;
import com.ssairen.backend.domain.responseaction.entity.ResponseActionType;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Service
public class DashboardNotificationService {

    private final DashboardSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public DashboardNotificationService(DashboardSessionRegistry sessionRegistry, ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    public void broadcastNewCase(FraudCase fraudCase) {
        broadcast(NewCaseEvent.of(CaseSummaryResponse.from(fraudCase)));
    }

    public void broadcastActionUpdate(Long caseId, ResponseActionType actionType, ResponseActionStatus status) {
        broadcast(ActionUpdateEvent.of(caseId, actionType, status));
    }

    private void broadcast(Object event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            log.error("대시보드 WebSocket 메시지 직렬화에 실패했습니다.", exception);
            return;
        }

        for (WebSocketSession session : sessionRegistry.getSessions()) {
            sendMessage(session, payload);
        }
    }

    private void sendMessage(WebSocketSession session, String payload) {
        synchronized (session) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (IOException exception) {
                    log.warn("대시보드 WebSocket 메시지 전송에 실패했습니다. socketId={}", session.getId(), exception);
                }
            }
        }
    }
}