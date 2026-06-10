package com.ssairen.backend.domain.callsession.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ssairen.backend.domain.callsession.dto.CallSessionResponse;
import com.ssairen.backend.domain.callsession.dto.CreateCallSessionRequest;
import com.ssairen.backend.domain.callsession.dto.SessionCompletionResult;
import com.ssairen.backend.domain.callsession.dto.TranscriptAcceptResult;
import com.ssairen.backend.domain.callsession.entity.CallSessionStatus;
import com.ssairen.backend.domain.callsession.repository.CallSessionRepository;
import com.ssairen.backend.domain.callsession.repository.TranscriptChunkRepository;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "ssairen.transcript.analysis-threshold-characters=5")
class CallSessionServiceTest {

    @Autowired
    private CallSessionService callSessionService;

    @Autowired
    private CallSessionRepository callSessionRepository;

    @Autowired
    private TranscriptChunkRepository transcriptChunkRepository;

    @BeforeEach
    void clearDatabase() {
        transcriptChunkRepository.deleteAll();
        callSessionRepository.deleteAll();
    }

    @Test
    void 같은_외부_통화_ID로_재요청하면_기존_세션을_반환한다() {
        CreateCallSessionRequest request = request("external-call-1");

        CallSessionResponse first = callSessionService.createSession(request);
        CallSessionResponse second = callSessionService.createSession(request);

        assertThat(second.sessionId()).isEqualTo(first.sessionId());
        assertThat(callSessionRepository.count()).isEqualTo(1);
    }

    @Test
    void STT_청크를_순서대로_저장하고_다음_sequence를_반환한다() {
        String sessionId = createSession();

        TranscriptAcceptResult result = callSessionService.acceptTranscript(
                sessionId,
                "chunk-1",
                1,
                "검찰 수사관입니다",
                0,
                1000,
                true
        );

        assertThat(result.acceptedSequence()).isEqualTo(1);
        assertThat(result.nextSequence()).isEqualTo(2);
        assertThat(result.analysisThresholdReached()).isTrue();
        assertThat(transcriptChunkRepository.count()).isEqualTo(1);
    }

    @Test
    void 동일한_청크를_재전송하면_중복_저장하지_않고_ACK_가능한_결과를_반환한다() {
        String sessionId = createSession();
        callSessionService.acceptTranscript(sessionId, "chunk-1", 1, "동일 텍스트", 0, 1000, true);

        TranscriptAcceptResult duplicate = callSessionService.acceptTranscript(
                sessionId,
                "chunk-1",
                1,
                "동일 텍스트",
                0,
                1000,
                true
        );

        assertThat(duplicate.duplicate()).isTrue();
        assertThat(duplicate.nextSequence()).isEqualTo(2);
        assertThat(transcriptChunkRepository.count()).isEqualTo(1);
    }

    @Test
    void 기대_sequence보다_큰_청크는_거부한다() {
        String sessionId = createSession();

        assertThatThrownBy(() -> callSessionService.acceptTranscript(
                sessionId,
                "chunk-2",
                2,
                "첫 청크가 누락됨",
                1000,
                2000,
                true
        ))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TRANSCRIPT_SEQUENCE_MISMATCH);
                    assertThat(exception.getDetails()).containsEntry("expectedSequence", 1L);
                });
    }

    @Test
    void 마지막_sequence까지_수신한_후_세션을_완료하고_마지막_분석_큐잉_여부를_반환한다() {
        String sessionId = createSession();
        callSessionService.acceptTranscript(sessionId, "chunk-1", 1, "마지막 청크", 0, 1000, true);

        SessionCompletionResult completed = callSessionService.completeSession(sessionId, OffsetDateTime.now(), 1);

        assertThat(completed.response().status()).isEqualTo(CallSessionStatus.COMPLETED);
        assertThat(completed.finalAnalysisQueued()).isTrue();
        assertThatThrownBy(() -> callSessionService.acceptTranscript(
                sessionId,
                "chunk-2",
                2,
                "종료 후 청크",
                1000,
                2000,
                true
        ))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CALL_SESSION_COMPLETED)
                );
    }

    private String createSession() {
        return callSessionService.createSession(request("external-call-" + System.nanoTime())).sessionId();
    }

    private CreateCallSessionRequest request(String externalCallId) {
        return new CreateCallSessionRequest(
                externalCallId,
                "device-1",
                OffsetDateTime.now(),
                "01012345678",
                new CreateCallSessionRequest.VictimRequest("김OO", 71)
        );
    }
}
