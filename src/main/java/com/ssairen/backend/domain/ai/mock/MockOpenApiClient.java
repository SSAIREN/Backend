package com.ssairen.backend.domain.ai.mock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisCommand;
import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisResult;
import com.ssairen.backend.domain.casefile.entity.PhishingType;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import com.ssairen.backend.global.logging.DebugExecutionTimer;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class MockOpenApiClient {

    private static final String SYSTEM_PROMPT = """
            You are a phishing-call risk scoring assistant used by a Spring backend.
            You must return only one valid JSON object with no markdown, no code fences, and no extra text.

            The JSON object must contain exactly these keys:
            - riskScore: integer from 0 to 100
            - phishingType: one of "AGENCY_IMPERSONATION", "ACCOUNT_TRANSFER_INDUCEMENT", "KIDNAPPING_THREAT", "REMOTE_APP_INSTALLATION", or null
            - aiSummary: concise Korean summary string, 1 to 2 sentences
            - keywords: JSON array of 1 to 5 short Korean keyword strings

            Decision rules:
            - Use only the provided transcript context.
            - Prefer null for phishingType when the type is unclear.
            - Keep aiSummary plain text with no bullets or labels.
            - Keep keywords specific to scam intent, pressure, money transfer, app install, agency impersonation, or threat language.
            - Never add any other properties.
            - Never wrap the JSON in markdown.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String chatPath;
    private final String apiKey;
    private final String model;

    public MockOpenApiClient(
            ObjectMapper objectMapper,
            @Value("${ssairen.analysis.mock-open-api.base-url:https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com}") String baseUrl,
            @Value("${ssairen.analysis.mock-open-api.chat-path:/v1beta/models/{model}:generateContent}") String chatPath,
            @Value("${ssairen.analysis.mock-open-api.api-key:}") String apiKey,
            @Value("${ssairen.analysis.mock-open-api.model:gemini-3.5-flash}") String model
    ) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.chatPath = chatPath;
        this.apiKey = apiKey;
        this.model = model;
    }

    public TranscriptAnalysisResult analyze(TranscriptAnalysisCommand command, String channel) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Mock open API key is not configured.",
                    Map.of("property", "ssairen.analysis.mock-open-api.api-key")
            );
        }

        try {
            GeminiGenerateContentResponse response = DebugExecutionTimer.measure(
                    log,
                    "external-rest",
                    "mockOpenApi.generateContent",
                    "channel=" + channel + ", model=" + model + ", sessionId=" + command.sessionId(),
                    () -> restClient.post()
                            .uri(builder -> builder
                                    .path(resolveChatPath())
                                    .queryParam("key", apiKey)
                                    .build())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(new GeminiGenerateContentRequest(
                                    new SystemInstruction(List.of(new Part(SYSTEM_PROMPT))),
                                    List.of(new Content(List.of(new Part(buildUserPrompt(command, channel))))),
                                    new GenerationConfig("application/json")
                            ))
                            .retrieve()
                            .body(GeminiGenerateContentResponse.class)
            );

            String content = extractContent(response);
            StructuredAnalysisResponse parsed = objectMapper.readValue(content, StructuredAnalysisResponse.class);

            return new TranscriptAnalysisResult(
                    clampRiskScore(parsed.riskScore()),
                    resolveType(parsed.phishingType()),
                    parsed.aiSummary(),
                    parsed.keywords() == null ? List.of() : parsed.keywords(),
                    "mock-open-api"
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Mock open API analysis request failed.",
                    Map.of("channel", channel, "message", exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage())
            );
        }
    }

    private String resolveChatPath() {
        return chatPath.replace("{model}", model);
    }

    private String buildUserPrompt(TranscriptAnalysisCommand command, String channel) {
        return """
                Analyze this phone-call transcript for phishing risk.
                Return JSON only.

                channel: %s
                sessionId: %s
                chunkId: %s
                sequence: %d
                victimName: %s
                victimAge: %s
                victimPhone: %s

                currentChunkTranscript:
                %s

                fullConversationContext:
                %s

                Output schema example:
                {
                  "riskScore": 72,
                  "phishingType": "ACCOUNT_TRANSFER_INDUCEMENT",
                  "aiSummary": "계좌 이체를 유도하는 정황이 반복되어 보이스피싱 위험이 있습니다.",
                  "keywords": ["계좌이체", "기관사칭", "압박"]
                }
                """.formatted(
                channel,
                command.sessionId(),
                command.chunkId(),
                command.sequence(),
                nullToEmpty(command.victimName()),
                command.victimAge() == null ? "" : command.victimAge(),
                nullToEmpty(command.victimPhone()),
                nullToEmpty(command.chunkTranscript()),
                nullToEmpty(command.conversationContext())
        );
    }

    private String extractContent(GeminiGenerateContentResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new IllegalStateException("Open API response is empty.");
        }

        Candidate firstCandidate = response.candidates().getFirst();
        if (firstCandidate.content() == null || firstCandidate.content().parts() == null || firstCandidate.content().parts().isEmpty()) {
            throw new IllegalStateException("Open API candidate content is empty.");
        }

        String text = firstCandidate.content().parts().stream()
                .map(Part::text)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");

        if (text.isBlank()) {
            throw new IllegalStateException("Open API message content is empty.");
        }
        return text;
    }

    private int clampRiskScore(Integer riskScore) {
        return Math.max(0, Math.min(100, riskScore == null ? 0 : riskScore));
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record GeminiGenerateContentRequest(
            SystemInstruction systemInstruction,
            List<Content> contents,
            GenerationConfig generationConfig
    ) {
    }

    private record SystemInstruction(List<Part> parts) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }

    private record GenerationConfig(String responseMimeType) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiGenerateContentResponse(List<Candidate> candidates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Candidate(Content content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StructuredAnalysisResponse(
            Integer riskScore,
            String phishingType,
            String aiSummary,
            List<String> keywords
    ) {
    }
}
