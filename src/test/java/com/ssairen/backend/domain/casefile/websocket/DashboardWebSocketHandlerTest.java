package com.ssairen.backend.domain.casefile.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssairen.backend.domain.user.entity.User;
import com.ssairen.backend.domain.user.entity.UserRole;
import com.ssairen.backend.domain.user.repository.UserRepository;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class DashboardWebSocketHandlerTest {

    @Mock
    private DashboardSessionRegistry sessionRegistry;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardWebSocketHandler dashboardWebSocketHandler;

    @Test
    void POLICE_사용자의_userId로_연결하면_세션을_등록한다() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.getUri()).willReturn(URI.create("ws://localhost/ws/dashboard?userId=3001"));
        given(userRepository.findByIdAndRole(3001L, UserRole.POLICE))
                .willReturn(Optional.of(policeUser(3001L)));

        dashboardWebSocketHandler.afterConnectionEstablished(session);

        verify(sessionRegistry).register(session);
    }

    @Test
    void userId_쿼리파라미터가_없으면_INVALID_REQUEST_예외를_던진다() {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.getUri()).willReturn(URI.create("ws://localhost/ws/dashboard"));

        assertThatThrownBy(() -> dashboardWebSocketHandler.afterConnectionEstablished(session))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST));

        verify(sessionRegistry, never()).register(any());
    }

    @Test
    void userId가_숫자가_아니면_INVALID_REQUEST_예외를_던진다() {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.getUri()).willReturn(URI.create("ws://localhost/ws/dashboard?userId=abc"));

        assertThatThrownBy(() -> dashboardWebSocketHandler.afterConnectionEstablished(session))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST));

        verify(sessionRegistry, never()).register(any());
    }

    @Test
    void POLICE_권한이_없는_userId면_USER_NOT_FOUND_예외를_던진다() {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.getUri()).willReturn(URI.create("ws://localhost/ws/dashboard?userId=1001"));
        given(userRepository.findByIdAndRole(1001L, UserRole.POLICE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardWebSocketHandler.afterConnectionEstablished(session))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));

        verify(sessionRegistry, never()).register(any());
    }

    @Test
    void 연결이_종료되면_세션을_제거한다() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);

        dashboardWebSocketHandler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(sessionRegistry).unregister(session);
    }

    @Test
    void 전송_오류가_발생하면_세션을_제거하고_연결을_닫는다() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.isOpen()).willReturn(true);

        dashboardWebSocketHandler.handleTransportError(session, new RuntimeException("boom"));

        verify(sessionRegistry).unregister(session);
        verify(session).close(CloseStatus.SERVER_ERROR);
    }

    private User policeUser(Long userId) {
        return User.mvpPreset(userId, "경찰관", UserRole.POLICE, null, null, null, OffsetDateTime.now());
    }
}