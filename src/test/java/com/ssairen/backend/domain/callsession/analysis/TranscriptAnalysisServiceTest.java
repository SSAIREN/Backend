package com.ssairen.backend.domain.callsession.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisCommand;
import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisResult;
import com.ssairen.backend.domain.callsession.entity.CallSession;
import com.ssairen.backend.domain.callsession.entity.TranscriptChunk;
import com.ssairen.backend.domain.callsession.repository.CallSessionRepository;
import com.ssairen.backend.domain.callsession.repository.TranscriptChunkRepository;
import com.ssairen.backend.domain.casefile.entity.FraudCase;
import com.ssairen.backend.domain.casefile.entity.PhishingType;
import com.ssairen.backend.domain.casefile.service.DashboardNotificationService;
import com.ssairen.backend.domain.notification.service.GuardianAlertService;
import com.ssairen.backend.domain.user.entity.User;
import com.ssairen.backend.domain.user.entity.UserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TranscriptAnalysisServiceTest {

    @Mock
    private CallSessionRepository callSessionRepository;

    @Mock
    private TranscriptChunkRepository transcriptChunkRepository;

    @Mock
    private TranscriptAnalysisGateway transcriptAnalysisGateway;

    @Mock
    private GuardianAlertService guardianAlertService;

    @Mock
    private DashboardNotificationService dashboardNotificationService;

    private TranscriptAnalysisService transcriptAnalysisService;

    @Captor
    private ArgumentCaptor<TranscriptAnalysisCommand> commandCaptor;

    @BeforeEach
    void setUp() {
        transcriptAnalysisService = new TranscriptAnalysisService(
                callSessionRepository,
                transcriptChunkRepository,
                transcriptAnalysisGateway,
                guardianAlertService,
                dashboardNotificationService,
                76
        );
    }

    @Test
    void analyzeRestChunk_groupsStoredChunksByChunkIdBeforeCallingGateway() {
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-06-13T10:00:00+09:00");
        User victim = User.mvpPreset(1001L, "Victim", UserRole.VICTIM, 71, "01012345678", null, startedAt);
        FraudCase fraudCase = new FraudCase(victim, startedAt);
        CallSession session = new CallSession("session-1", "external-1", "device-1", victim, fraudCase, startedAt);

        List<TranscriptChunk> storedChunks = List.of(
                new TranscriptChunk("chunk-1", session, 1L, "first sentence", 0L, 1000L, false),
                new TranscriptChunk("chunk-2", session, 2L, "bank staff speaking", 1000L, 2000L, true),
                new TranscriptChunk("chunk-1", session, 3L, "send money now", 2000L, 3000L, true)
        );

        given(callSessionRepository.findByIdForUpdate("session-1")).willReturn(Optional.of(session));
        given(transcriptChunkRepository.findAllByCallSessionIdAndSequenceLessThanEqualOrderBySequenceAsc("session-1", 3L))
                .willReturn(storedChunks);
        given(transcriptAnalysisGateway.analyzeRest(any(TranscriptAnalysisCommand.class)))
                .willReturn(new TranscriptAnalysisResult(
                        88,
                        PhishingType.ACCOUNT_TRANSFER_INDUCEMENT,
                        "Risk is high.",
                        List.of("transfer", "bank"),
                        "mock-open-api"
                ));

        transcriptAnalysisService.analyzeRestChunk("session-1", "chunk-1", 3L);

        verify(transcriptAnalysisGateway).analyzeRest(commandCaptor.capture());
        TranscriptAnalysisCommand command = commandCaptor.getValue();

        assertEquals("chunk-1", command.chunkId());
        assertEquals("first sentence send money now", command.chunkTranscript());
        assertEquals("first sentence send money now\nbank staff speaking", command.conversationContext());
    }
}
