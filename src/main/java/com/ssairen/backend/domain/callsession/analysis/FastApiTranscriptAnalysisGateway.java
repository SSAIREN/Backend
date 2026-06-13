package com.ssairen.backend.domain.callsession.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisCommand;
import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisResult;
import com.ssairen.backend.domain.casefile.entity.PhishingType;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "ssairen.analysis.provider", havingValue = "fastapi")
public class FastApiTranscriptAnalysisGateway implements TranscriptAnalysisGateway {

    private final RestClient restClient;
    private final String restAnalysisPath;
    private final String websocketAnalysisPath;

    public FastApiTranscriptAnalysisGateway(
            @Value("${ssairen.analysis.fastapi.url:http://localhost:8000}") String baseUrl,
            @Value("${ssairen.analysis.fastapi.rest-path:/api/v1/call-analysis/rest}") String restAnalysisPath,
            @Value("${ssairen.analysis.fastapi.websocket-path:/api/v1/call-analysis/websocket}") String websocketAnalysisPath
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.restAnalysisPath = restAnalysisPath;
        this.websocketAnalysisPath = websocketAnalysisPath;
    }

    @Override
    public TranscriptAnalysisResult analyzeRest(TranscriptAnalysisCommand command) {
        return requestAnalysis(command, restAnalysisPath, "rest");
    }

    @Override
    public TranscriptAnalysisResult analyzeWebSocket(TranscriptAnalysisCommand command) {
        return requestAnalysis(command, websocketAnalysisPath, "websocket");
    }

    private TranscriptAnalysisResult requestAnalysis(
            TranscriptAnalysisCommand command,
            String path,
            String channel
    ) {
        try {
            FastApiAnalysisResponse response = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new FastApiAnalysisRequest(
                            command.sessionId(),
                            command.chunkId(),
                            command.sequence(),
                            command.chunkTranscript(),
                            command.conversationContext(),
                            command.victimName(),
                            command.victimAge(),
                            command.victimPhone(),
                            channel
                    ))
                    .retrieve()
                    .body(FastApiAnalysisResponse.class);

            if (response == null) {
                throw new IllegalStateException("FastAPI response is empty.");
            }

            return new TranscriptAnalysisResult(
                    Math.max(0, Math.min(100, response.riskScore == null ? 0 : response.riskScore)),
                    resolveType(response.phishingType),
                    response.aiSummary,
                    response.keywords == null ? List.of() : response.keywords,
                    "fastapi"
            );
        } catch (Exception exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "FastAPI analysis request failed.",
                    Map.of("channel", channel, "message", exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage())
            );
        }
    }

    private PhishingType resolveType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PhishingType.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private record FastApiAnalysisRequest(
            String sessionId,
            String chunkId,
            long sequence,
            String chunkTranscript,
            String conversationContext,
            String victimName,
            Integer victimAge,
            String victimPhone,
            String channel
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FastApiAnalysisResponse(
            Integer riskScore,
            String phishingType,
            String aiSummary,
            List<String> keywords
    ) {
    }
}
