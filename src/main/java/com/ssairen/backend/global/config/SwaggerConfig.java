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
import java.util.List;
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
                                Flutter 피해자 앱과 Spring Boot 사이의 통화 세션 및 STT WebSocket 계약입니다.

                                - REST API: 통화 세션 생성 및 상태 복구
                                - WebSocket: STT 청크 전송, ACK/NACK, 통화 종료
                                - FastAPI 및 경찰 Dashboard 연동은 현재 문서 범위에 포함하지 않습니다.
                                """)
                        .version("v1.0.0"))
                .tags(List.of(
                        new Tag().name("통화 세션").description("Flutter 통화 세션 생성 및 상태 조회"),
                        new Tag().name("피해자 WebSocket").description("Flutter 피해자 앱 양방향 WebSocket 메시지 계약")
                ))
                .paths(webSocketPaths());
    }

    /**
     * OpenAPI는 WebSocket 메시지 프로토콜을 직접 표현하지 못한다.
     * Swagger UI에서 Flutter 개발자가 연결 주소와 송수신 예시를 확인할 수 있도록
     * handshake 경로를 문서 전용 PathItem으로 등록한다.
     */
    private Paths webSocketPaths() {
        Operation operation = new Operation()
                .tags(List.of("피해자 WebSocket"))
                .summary("피해자 앱 STT 양방향 WebSocket 연결")
                .description("""
                        연결 주소: `ws://{host}/ws/v1/victim?sessionId={sessionId}`

                        Swagger UI의 Try it out으로 WebSocket 연결을 실행할 수는 없습니다.

                        연결 직후 서버는 `SESSION_READY` 이벤트로 다음 STT sequence를 전달합니다.
                        Flutter는 `TRANSCRIPT_CHUNK`, `SESSION_COMPLETE`, `PING` 이벤트를 전송합니다.
                        서버는 `TRANSCRIPT_ACK`, `TRANSCRIPT_NACK`, `SESSION_COMPLETE_ACK`, `PONG` 이벤트를 반환합니다.
                        """)
                .addParametersItem(new Parameter()
                        .name("sessionId")
                        .in("query")
                        .required(true)
                        .description("통화 세션 생성 API에서 발급받은 sessionId")
                        .example("550e8400-e29b-41d4-a716-446655440000"))
                .responses(new ApiResponses()
                        .addApiResponse("101", new ApiResponse()
                                .description("WebSocket 연결 성공")
                                .content(new Content().addMediaType(
                                        "application/json",
                                        new MediaType()
                                                .addExamples("Flutter -> SpringBoot: TRANSCRIPT_CHUNK", transcriptChunkExample())
                                                .addExamples("SpringBoot -> Flutter: TRANSCRIPT_ACK", transcriptAckExample())
                                                .addExamples("Flutter -> SpringBoot: SESSION_COMPLETE", sessionCompleteExample())
                                )))
                        .addApiResponse("400", new ApiResponse().description("sessionId 누락 또는 잘못된 handshake 요청"))
                        .addApiResponse("404", new ApiResponse().description("통화 세션을 찾을 수 없음")));

        return new Paths().addPathItem("/ws/v1/victim", new PathItem().get(operation));
    }

    private Example transcriptChunkExample() {
        return new Example()
                .summary("확정된 STT 텍스트 청크 전송")
                .value("""
                        {
                          "eventId": "event-001",
                          "eventType": "TRANSCRIPT_CHUNK",
                          "sessionId": "550e8400-e29b-41d4-a716-446655440000",
                          "occurredAt": "2026-06-10T15:20:47+09:00",
                          "data": {
                            "chunkId": "chunk-001",
                            "sequence": 1,
                            "text": "검찰 수사관입니다.",
                            "startedAtMs": 0,
                            "endedAtMs": 3000,
                            "isFinal": true
                          }
                        }
                        """);
    }

    private Example transcriptAckExample() {
        return new Example()
                .summary("STT 청크 DB 저장 완료 ACK")
                .value("""
                        {
                          "eventId": "server-event-001",
                          "eventType": "TRANSCRIPT_ACK",
                          "sessionId": "550e8400-e29b-41d4-a716-446655440000",
                          "occurredAt": "2026-06-10T15:20:47+09:00",
                          "data": {
                            "chunkId": "chunk-001",
                            "acceptedSequence": 1,
                            "nextTranscriptSequence": 2,
                            "duplicate": false,
                            "analysisThresholdReached": false
                          }
                        }
                        """);
    }

    private Example sessionCompleteExample() {
        return new Example()
                .summary("마지막 청크 ACK 확인 후 통화 종료 요청")
                .value("""
                        {
                          "eventId": "event-002",
                          "eventType": "SESSION_COMPLETE",
                          "sessionId": "550e8400-e29b-41d4-a716-446655440000",
                          "occurredAt": "2026-06-10T15:32:00+09:00",
                          "data": {
                            "endedAt": "2026-06-10T15:32:00+09:00",
                            "lastTranscriptSequence": 42
                          }
                        }
                        """);
    }
}
