package com.ssairen.backend.domain.ai.mock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisCommand;
import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisResult;
import com.ssairen.backend.domain.casefile.entity.PhishingType;
import com.ssairen.backend.global.error.BusinessException;
import com.ssairen.backend.global.error.ErrorCode;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
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
            @Value("${ssairen.analysis.mock-open-api.base-url:https://api.openai.com}") String baseUrl,
            @Value("${ssairen.analysis.mock-open-api.chat-path:/v1/chat/completions}") String chatPath,
            @Value("${ssairen.analysis.mock-open-api.api-key:}") String apiKey,
            @Value("${ssairen.analysis.mock-open-api.model:gpt-4o-mini}") String model
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
            OpenAiChatCompletionResponse response = restClient.post()
                    .uri(chatPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(new OpenAiChatCompletionRequest(
                            model,
                            List.of(
                                    new ChatMessage("system", SYSTEM_PROMPT),
                                    new ChatMessage("user", buildUserPrompt(command, channel))
                            ),
                            new ResponseFormat("json_object")
                    ))
                    .retrieve()
                    .body(OpenAiChatCompletionResponse.class);

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
                  "aiSummary": "계좌 이체를 유도하는 정황이 반복되어 보이스피싱 위험이 높습니다.",
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

    private String extractContent(OpenAiChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("Open API response is empty.");
        }

        ChatChoice firstChoice = response.choices().getFirst();
        if (firstChoice.message() == null || firstChoice.message().content() == null || firstChoice.message().content().isBlank()) {
            throw new IllegalStateException("Open API message content is empty.");
        }
        return firstChoice.message().content();
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

    private record OpenAiChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            ResponseFormat response_format
    ) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ResponseFormat(String type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiChatCompletionResponse(List<ChatChoice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatChoice(ChatMessageContent message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatMessageContent(String content) {
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
