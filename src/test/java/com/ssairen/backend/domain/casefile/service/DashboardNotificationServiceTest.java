package com.ssairen.backend.domain.casefile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ssairen.backend.domain.casefile.entity.FraudCase;
import com.ssairen.backend.domain.casefile.websocket.DashboardSessionRegistry;
import com.ssairen.backend.domain.responseaction.entity.ResponseActionStatus;
import com.ssairen.backend.domain.responseaction.entity.ResponseActionType;
import com.ssairen.backend.domain.user.entity.User;
import com.ssairen.backend.domain.user.entity.UserRole;
import java.time.OffsetDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class DashboardNotificationServiceTest {

    @Mock
    private DashboardSessionRegistry sessionRegistry;

    private DashboardNotificationService dashboardNotificationService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        dashboardNotificationService = new DashboardNotificationService(sessionRegistry, objectMapper);
    }

    @Test
    void 신규_케이스를_연결된_세션에_브로드캐스트한다() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.isOpen()).willReturn(true);
        given(sessionRegistry.getSessions()).willReturn(Set.of(session));

        FraudCase fraudCase = new FraudCase(victim(), OffsetDateTime.now());

        dashboardNotificationService.broadcastNewCase(fraudCase);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        String payload = captor.getValue().getPayload();
        assertThat(payload).contains("\"type\":\"NEW_CASE\"");
        assertThat(payload).contains("\"case\":");
        assertThat(payload).contains("\"victimName\":\"피해자A\"");
    }

    @Test
    void 대응조치_진행상황을_연결된_세션에_브로드캐스트한다() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.isOpen()).willReturn(true);
        given(sessionRegistry.getSessions()).willReturn(Set.of(session));

        dashboardNotificationService.broadcastActionUpdate(1L, ResponseActionType.GPS, ResponseActionStatus.COMPLETED);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        String payload = captor.getValue().getPayload();
        assertThat(payload).contains("\"type\":\"ACTION_UPDATE\"");
        assertThat(payload).contains("\"caseId\":1");
        assertThat(payload).contains("\"actionType\":\"GPS\"");
        assertThat(payload).contains("\"status\":\"COMPLETED\"");
    }

    @Test
    void 닫혀있는_세션에는_메시지를_전송하지_않는다() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        given(session.isOpen()).willReturn(false);
        given(sessionRegistry.getSessions()).willReturn(Set.of(session));

        dashboardNotificationService.broadcastActionUpdate(1L, ResponseActionType.SMS, ResponseActionStatus.PENDING);

        verify(session, never()).sendMessage(any());
    }

    private User victim() {
        return User.mvpPreset(1001L, "피해자A", UserRole.VICTIM, 70, "010-1111-2222", null, OffsetDateTime.now());
    }
}