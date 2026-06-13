package com.ssairen.backend.domain.callsession.websocket;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class VictimSessionRegistry {

    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsByCallSessionId = new ConcurrentHashMap<>();

    public void register(String callSessionId, WebSocketSession session) {
        sessionsByCallSessionId
                .computeIfAbsent(callSessionId, ignored -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    public void unregister(String callSessionId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByCallSessionId.get(callSessionId);
        if (sessions == null) {
            return;
        }

        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByCallSessionId.remove(callSessionId, sessions);
        }
    }

    public Set<WebSocketSession> getSessions(String callSessionId) {
        Set<WebSocketSession> sessions = sessionsByCallSessionId.get(callSessionId);
        if (sessions == null) {
            return Set.of();
        }
        return Collections.unmodifiableSet(sessions);
    }
}
