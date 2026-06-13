package com.ssairen.backend.domain.ai.use.client;

import com.ssairen.backend.domain.ai.use.dto.AiDemoRunRequest;
import com.ssairen.backend.domain.ai.use.dto.AiDemoRunResponse;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * AI(FastAPI)의 데모 파이프라인 트리거 endpoint(POST /api/v1/pipeline-a/runs/demo)를 호출하는 RestClient다.
 */
@Component
public class AiDemoClient {

    private final RestClient restClient;
    private final String demoPath;

    public AiDemoClient(
            @Value("${ssairen.ai.use.fastapi-url:http://localhost:8000}") String baseUrl,
            @Value("${ssairen.ai.use.demo-path:/api/v1/pipeline-a/runs/demo}") String demoPath
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.demoPath = demoPath;
    }

    public AiDemoRunResponse runDemo(AiDemoRunRequest request) {
        try {
            AiDemoRunResponse response = restClient.post()
                    .uri(demoPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiDemoRunResponse.class);

            if (response == null) {
                throw new IllegalStateException("FastAPI demo response is empty.");
            }
            return response;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "FastAPI demo trigger failed.",
                    Map.of("message", exception.getMessage() == null
                            ? exception.getClass().getSimpleName()
                            : exception.getMessage())
            );
        }
    }
}
