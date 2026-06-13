package com.ssairen.backend.domain.ai.use.service;

import com.ssairen.backend.domain.ai.use.client.AiDemoClient;
import com.ssairen.backend.domain.ai.use.dto.AiAnalysisCallbackRequest;
import com.ssairen.backend.domain.ai.use.dto.AiDemoRunRequest;
import com.ssairen.backend.domain.ai.use.dto.AiDemoRunResponse;
import com.ssairen.backend.domain.ai.use.dto.AiTriggerRequest;
import com.ssairen.backend.domain.ai.use.dto.AiTriggerResponse;
import com.ssairen.backend.domain.callsession.entity.TranscriptChunk;
import com.ssairen.backend.domain.callsession.repository.TranscriptChunkRepository;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 데모 오케스트레이션 서비스다. FE 트리거를 FastAPI 데모 호출로 중계하고, AI 종합 결과 콜백을 수신한다.
 */
@Service
public class AiUseService {

    private static final Logger log = LoggerFactory.getLogger(AiUseService.class);

    private final AiDemoClient aiDemoClient;
    private final TranscriptChunkRepository transcriptChunkRepository;

    public AiUseService(AiDemoClient aiDemoClient, TranscriptChunkRepository transcriptChunkRepository) {
        this.aiDemoClient = aiDemoClient;
        this.transcriptChunkRepository = transcriptChunkRepository;
    }

    @Transactional(readOnly = true)
    public AiTriggerResponse triggerDemo(AiTriggerRequest request) {
        // FE는 message 전문을 가지고 있지 않으므로, request.message() 는 무시한다.
        // sessionId 로 저장된 STT 청크를 합쳐 통화 전문을 만들어 AI 데모 입력으로 전달한다.
        String transcript = buildTranscript(request.sessionId());
        AiDemoRunRequest demoRequest = new AiDemoRunRequest(transcript, request.sessionId());
        // FastAPI 가 session_id 를 정상 반환(작업 접수 성공)하면 FE 에는 성공으로 응답한다.
        // 실제 분석 결과는 이후 /ai/use/callback 으로 비동기 전달된다.
        AiDemoRunResponse response = aiDemoClient.runDemo(demoRequest);
        return new AiTriggerResponse(response.sessionId(), "SUCCESS");
    }

    /**
     * sessionId 로 저장된 모든 STT 청크를 sequence 순으로 모아 통화 전문으로 합친다.
     * 같은 chunkId 끼리는 공백으로, 서로 다른 chunkId 는 줄바꿈으로 연결한다.
     */
    private String buildTranscript(String sessionId) {
        List<TranscriptChunk> chunks =
                transcriptChunkRepository.findAllByCallSessionIdOrderBySequenceAsc(sessionId);
        if (chunks.isEmpty()) {
            log.warn("AI 데모 트리거: sessionId={} 에 저장된 STT 청크가 없어 전문을 만들 수 없습니다.", sessionId);
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "No transcript chunks are stored for the session.",
                    Map.<String, Object>of("sessionId", sessionId)
            );
        }

        Map<String, StringJoiner> groupedByChunkId = new LinkedHashMap<>();
        for (TranscriptChunk chunk : chunks) {
            groupedByChunkId
                    .computeIfAbsent(chunk.getChunkId(), ignored -> new StringJoiner(" "))
                    .add(chunk.getText().trim());
        }

        return groupedByChunkId.values().stream()
                .map(StringJoiner::toString)
                .filter(text -> !text.isBlank())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    public void handleCallback(AiAnalysisCallbackRequest request) {
        log.info("AI 종합 결과 콜백 수신: sessionId={}, riskLevel={}, riskScore={}",
                request.sessionId(), request.riskLevel(), request.riskScore());
        // TODO: 추후 FraudCase 갱신 / 대시보드(WebSocket) 통지 / FE 결과 전달 처리. 현재는 수신만 하고 아무 동작 안 함.
    }
}
