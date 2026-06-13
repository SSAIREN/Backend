package com.ssairen.backend.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SSAIREN Flutter - Spring Boot API")
                        .description("""
                                Flutter 앱과 Spring Boot 백엔드 사이의 MVP 통신 계약 문서입니다.

                                - REST 1단계: 통화 시작 직후 5초 단위 STT를 REST API로 업로드하고, Spring이 FastAPI 분석 서버에 전달한 뒤 위험도 결과를 Flutter에 반환합니다.
                                - WebSocket 2단계: REST 분석 결과로 실시간 관찰이 필요하다고 판단되면, Flutter가 WebSocket으로 전환하여 추가 STT를 계속 전송합니다.
                                - 보호자 알림: 이상도가 임계치를 넘으면 Spring이 현재 userId와 연결된 보호자에게 FCM 알림 전송을 시도합니다.
                                """)
                        .version("v1.1.1"))
                .tags(List.of(
                        new Tag().name("통화 세션").description("세션 생성, 세션 상태 조회, 초기 REST 기반 STT 분석 API"),
                        new Tag().name("피해자 WebSocket").description("REST 분석 이후 실시간 감시 단계에서 사용하는 WebSocket 계약"),
                        new Tag().name("경찰 대시보드 WebSocket").description("경찰 관제 대시보드에서 신규 케이스 및 대응 조치 현황을 실시간으로 수신하는 WebSocket 계약")
                ))
                .paths(webSocketPaths());
    }

    /**
     * OpenAPI 스펙은 WebSocket 프로토콜 자체를 완전하게 표현하지 못한다.
     * 그래서 Flutter 개발자가 handshake 주소와 메시지 예시를 Swagger UI에서 바로 확인할 수 있도록
     * 문서 전용 PathItem을 수동으로 추가한다.
     */
    private Paths webSocketPaths() {
        Operation operation = new Operation()
                .tags(List.of("피해자 WebSocket"))
                .summary("실시간 고위험 모니터링용 WebSocket 연결")
                .description("""
                        연결 주소: `ws://{host}/ws/v1/victim?sessionId={sessionId}`

                        이 경로는 REST 초기 분석 응답에서 `shouldOpenWebSocket` 값이 `true`일 때 사용합니다.
                        연결 직후 서버는 `SESSION_READY` 이벤트로 다음에 받아야 하는 STT sequence를 알려줍니다.
                        Flutter는 `TRANSCRIPT_CHUNK`, `SESSION_COMPLETE`, `PING` 이벤트를 전송합니다.
                        서버는 `TRANSCRIPT_ACK`, `TRANSCRIPT_NACK`, `ANALYSIS_RESULT`, `ANALYSIS_ERROR`, `SESSION_COMPLETE_ACK`, `PONG` 이벤트를 반환합니다.
                        """)
                .addParametersItem(new Parameter()
                        .name("sessionId")
                        .in("query")
                        .required(true)
                        .description("통화 세션 생성 API에서 발급받은 sessionId")
                        .example("550e8400-e29b-41d4-a716-446655440000"))
                .responses(new ApiResponses()
                        .addApiResponse("101", new ApiResponse()
                                .description("WebSocket handshake 성공")
                                .content(new Content().addMediaType(
                                        "application/json",
                                        new MediaType()
                                                .addExamples("Flutter -> SpringBoot: TRANSCRIPT_CHUNK", transcriptChunkExample())
                                                .addExamples("SpringBoot -> Flutter: TRANSCRIPT_ACK", transcriptAckExample())
                                                .addExamples("Flutter -> SpringBoot: SESSION_COMPLETE", sessionCompleteExample())
                                )))
                        .addApiResponse("400", new ApiResponse().description("sessionId 누락 또는 잘못된 handshake 요청"))
                        .addApiResponse("404", new ApiResponse().description("해당 통화 세션을 찾을 수 없음")));

        return new Paths()
                .addPathItem("/ws/v1/victim", new PathItem().get(operation))
                .addPathItem("/ws/dashboard", new PathItem().get(dashboardOperation()));
    }

    private Operation dashboardOperation() {
        return new Operation()
                .tags(List.of("경찰 대시보드 WebSocket"))
                .summary("경찰 관제 대시보드용 실시간 알림 WebSocket 연결")
                .description("""
                        연결 주소: `ws://{host}/ws/dashboard?userId={userId}`

                        `userId`는 POLICE 권한을 가진 사용자의 ID여야 합니다.
                        연결에 성공하면 서버는 신규 케이스 발생 시 `NEW_CASE` 이벤트를,
                        대응 조치 진행상황이 변경되면 `ACTION_UPDATE` 이벤트를 브로드캐스트합니다.
                        클라이언트가 별도로 전송해야 하는 메시지는 없습니다.
                        """)
                .addParametersItem(new Parameter()
                        .name("userId")
                        .in("query")
                        .required(true)
                        .description("POLICE 권한을 가진 사용자 ID")
                        .example("3001"))
                .responses(new ApiResponses()
                        .addApiResponse("101", new ApiResponse()
                                .description("WebSocket handshake 성공")
                                .content(new Content().addMediaType(
                                        "application/json",
                                        new MediaType()
                                                .addExamples("SpringBoot -> Dashboard: NEW_CASE", newCaseExample())
                                                .addExamples("SpringBoot -> Dashboard: ACTION_UPDATE", actionUpdateExample())
                                )))
                        .addApiResponse("400", new ApiResponse().description("userId 누락 또는 숫자가 아닌 잘못된 handshake 요청"))
                        .addApiResponse("404", new ApiResponse().description("POLICE 권한을 가진 사용자를 찾을 수 없음")));
    }

    private Example newCaseExample() {
        return new Example()
                .summary("신규 케이스 발생 이벤트 예시")
                .value(orderedMap(
                        entry("type", "NEW_CASE"),
                        entry("case", orderedMap(
                                entry("caseId", 1),
                                entry("victimName", "김OO"),
                                entry("age", 71),
                                entry("riskScore", 85),
                                entry("phishingType", "KIDNAPPING_THREAT"),
                                entry("status", "IN_PROGRESS"),
                                entry("keywords", "검찰, 계좌 이체, 안전 계좌"),
                                entry("region", "서울특별시 강남구"),
                                entry("callDurationSec", 180),
                                entry("detectedAt", "2026-06-10T15:20:00+09:00")
                        ))
                ));
    }

    private Example actionUpdateExample() {
        return new Example()
                .summary("대응 조치 진행상황 변경 이벤트 예시")
                .value(orderedMap(
                        entry("type", "ACTION_UPDATE"),
                        entry("caseId", 1),
                        entry("actionType", "GPS"),
                        entry("status", "COMPLETED")
                ));
    }

    private Example transcriptChunkExample() {
        return new Example()
                .summary("실시간 STT 청크 전송 예시")
                .value(orderedMap(
                        entry("eventId", "event-001"),
                        entry("eventType", "TRANSCRIPT_CHUNK"),
                        entry("sessionId", "550e8400-e29b-41d4-a716-446655440000"),
                        entry("occurredAt", "2026-06-10T15:20:47+09:00"),
                        entry("data", orderedMap(
                                entry("chunkId", "chunk-001"),
                                entry("sequence", 8),
                                entry("text", "검찰 수사관입니다. 지금 앱을 설치하세요."),
                                entry("startedAtMs", 35000),
                                entry("endedAtMs", 40000),
                                entry("isFinal", true)
                        ))
                ));
    }

    private Example transcriptAckExample() {
        return new Example()
                .summary("실시간 STT 수신 ACK 예시")
                .value(orderedMap(
                        entry("eventId", "server-event-001"),
                        entry("eventType", "TRANSCRIPT_ACK"),
                        entry("sessionId", "550e8400-e29b-41d4-a716-446655440000"),
                        entry("occurredAt", "2026-06-10T15:20:47+09:00"),
                        entry("data", orderedMap(
                                entry("chunkId", "chunk-001"),
                                entry("acceptedSequence", 8),
                                entry("nextTranscriptSequence", 9),
                                entry("duplicate", false),
                                entry("analysisThresholdReached", true)
                        ))
                ));
    }

    private Example sessionCompleteExample() {
        return new Example()
                .summary("통화 종료 알림 예시")
                .value(orderedMap(
                        entry("eventId", "event-002"),
                        entry("eventType", "SESSION_COMPLETE"),
                        entry("sessionId", "550e8400-e29b-41d4-a716-446655440000"),
                        entry("occurredAt", "2026-06-10T15:32:00+09:00"),
                        entry("data", orderedMap(
                                entry("endedAt", "2026-06-10T15:32:00+09:00"),
                                entry("lastTranscriptSequence", 42)
                        ))
                ));
    }

    /**
     * Swagger 예시를 문자열이 아닌 실제 JSON 객체 형태로 보여주기 위해
     * 삽입 순서가 유지되는 LinkedHashMap을 사용한다.
     */
    @SafeVarargs
    private final Map<String, Object> orderedMap(Map.Entry<String, Object>... entries) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private Map.Entry<String, Object> entry(String key, Object value) {
        return Map.entry(key, value);
    }
}
