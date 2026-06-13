package com.ssairen.backend.domain.casefile.websocket;

import com.ssairen.backend.domain.user.entity.UserRole;
import com.ssairen.backend.domain.user.repository.UserRepository;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class DashboardWebSocketHandler extends TextWebSocketHandler {

    private final DashboardSessionRegistry sessionRegistry;
    private final UserRepository userRepository;

    public DashboardWebSocketHandler(DashboardSessionRegistry sessionRegistry, UserRepository userRepository) {
        this.sessionRegistry = sessionRegistry;
        this.userRepository = userRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractUserId(session);
        userRepository.findByIdAndRole(userId, UserRole.POLICE)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "경찰 사용자를 찾을 수 없습니다."));

        sessionRegistry.register(session);

        log.debug("Dashboard WebSocket connected. userId={}, socketId={}, remoteAddress={}",
                userId, session.getId(), session.getRemoteAddress());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionRegistry.unregister(session);
        log.debug("Dashboard WebSocket closed. socketId={}, closeCode={}, reason={}",
                session.getId(), status.getCode(), status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessionRegistry.unregister(session);
        log.debug("Dashboard WebSocket transport error. socketId={}, message={}",
                session.getId(), exception.getMessage(), exception);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private Long extractUserId(WebSocketSession session) {
        String userId = UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("userId");
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "WebSocket connection requires a userId.");
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "userId must be numeric.");
        }
    }
}