package com.ssairen.backend.domain.ai.mock;

import com.ssairen.backend.domain.callsession.analysis.TranscriptAnalysisGateway;
import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisCommand;
import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ssairen.analysis.provider", havingValue = "mock-open-api", matchIfMissing = true)
public class MockOpenApiTranscriptAnalysisGateway implements TranscriptAnalysisGateway {

    private final MockOpenApiClient mockOpenApiClient;

    public MockOpenApiTranscriptAnalysisGateway(MockOpenApiClient mockOpenApiClient) {
        this.mockOpenApiClient = mockOpenApiClient;
    }

    @Override
    public TranscriptAnalysisResult analyzeRest(TranscriptAnalysisCommand command) {
        return mockOpenApiClient.analyze(command, "rest");
    }

    @Override
    public TranscriptAnalysisResult analyzeWebSocket(TranscriptAnalysisCommand command) {
        return mockOpenApiClient.analyze(command, "websocket");
    }
}
