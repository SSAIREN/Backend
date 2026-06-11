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
                                Flutter 피해자 앱과 Spring Boot 사이의 통화 모니터링 계약 문서입니다.

                                - REST 1단계: 통화 시작 직후 5초 단위 STT를 REST로 업로드하고 FastAPI 일반 분석 결과를 받습니다.
                                - WebSocket 2단계: 위험도 상승 이후 실시간 STT를 WebSocket으로 올리고 FastAPI 실시간 분석 결과를 받습니다.
                                - 관리자 대시보드 및 기타 내부 운영 API는 현재 문서 범위에 포함하지 않습니다.
                                """)
                        .version("v1.1.0"))
                .tags(List.of(
                        new Tag().name("통화 세션").description("세션 생성, 상태 조회, 초기 REST 분석 API"),
                        new Tag().name("피해자 WebSocket").description("위험도 상승 이후 실시간 STT 업로드 WebSocket 계약")
                ))
                .paths(webSocketPaths());
    }

    /**
     * OpenAPI는 WebSocket 프로토콜을 완전하게 모델링하지 못한다.
     * 그래서 Flutter 개발자가 handshake 주소와 대표 메시지 예시를 바로 확인할 수 있도록
     * 문서 전용 PathItem을 수동으로 추가한다.
     */
    private Paths webSocketPaths() {
        Operation operation = new Operation()
                .tags(List.of("피해자 WebSocket"))
                .summary("고위험 단계 STT 실시간 업로드 WebSocket 연결")
                .description("""
                        연결 주소: `ws://{host}/ws/v1/victim?sessionId={sessionId}`

                        이 경로는 REST 초기 분석 단계에서 위험도가 특정 threshold 이상일 때 사용합니다.
                        연결 직후 서버는 `SESSION_READY` 이벤트로 다음 STT sequence를 내려줍니다.
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
                .summary("실시간 STT 청크 전송")
                .value("""
                        {
                          "eventId": "event-001",
                          "eventType": "TRANSCRIPT_CHUNK",
                          "sessionId": "550e8400-e29b-41d4-a716-446655440000",
                          "occurredAt": "2026-06-10T15:20:47+09:00",
                          "data": {
                            "chunkId": "chunk-001",
                            "sequence": 8,
                            "text": "검찰 수사관입니다. 지금 앱을 설치하세요.",
                            "startedAtMs": 35000,
                            "endedAtMs": 40000,
                            "isFinal": true
                          }
                        }
                        """);
    }

    private Example transcriptAckExample() {
        return new Example()
                .summary("실시간 STT 저장 ACK")
                .value("""
                        {
                          "eventId": "server-event-001",
                          "eventType": "TRANSCRIPT_ACK",
                          "sessionId": "550e8400-e29b-41d4-a716-446655440000",
                          "occurredAt": "2026-06-10T15:20:47+09:00",
                          "data": {
                            "chunkId": "chunk-001",
                            "acceptedSequence": 8,
                            "nextTranscriptSequence": 9,
                            "duplicate": false,
                            "analysisThresholdReached": true
                          }
                        }
                        """);
    }

    private Example sessionCompleteExample() {
        return new Example()
                .summary("마지막 실시간 청크 이후 통화 종료 요청")
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
