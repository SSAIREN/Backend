package com.ssairen.backend.domain.callsession.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.ssairen.backend.domain.callsession.analysis.TranscriptAnalysisGateway;
import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisCommand;
import com.ssairen.backend.domain.callsession.analysis.dto.TranscriptAnalysisResult;
import com.ssairen.backend.domain.casefile.entity.PhishingType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "ssairen.analysis.websocket-escalation-threshold=70")
@AutoConfigureMockMvc
class CallSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TranscriptAnalysisGateway transcriptAnalysisGateway;

    @Test
    void Flutter_세션_생성_요청을_userId_기반_계약으로_받는다() throws Exception {
        mockMvc.perform(post("/api/mobile/call-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1001,
                                  "externalCallId": "device-call-controller-test",
                                  "deviceId": "victim-device-001",
                                  "startedAt": "2026-06-10T15:20:00+09:00",
                                  "phoneNumber": "01012345678",
                                  "victim": {
                                    "name": "김영희",
                                    "age": 71
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.nextTranscriptSequence").value(1))
                .andExpect(jsonPath("$.webSocketUrl").value(org.hamcrest.Matchers.containsString("/ws/v1/victim?sessionId=")));
    }

    @Test
    void Flutter가_초기_STT를_REST로_보내면_FastAPI_분석_결과를_같이_반환한다() throws Exception {
        given(transcriptAnalysisGateway.analyzeRest(any(TranscriptAnalysisCommand.class)))
                .willReturn(new TranscriptAnalysisResult(
                        82,
                        PhishingType.ACCOUNT_TRANSFER_INDUCEMENT,
                        "계좌이체 유도 표현이 반복됩니다.",
                        List.of("안전계좌", "이체", "검찰"),
                        "fastapi"
                ));

        String sessionResponse = mockMvc.perform(post("/api/mobile/call-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1001,
                                  "externalCallId": "device-call-rest-analysis-test",
                                  "deviceId": "victim-device-002",
                                  "startedAt": "2026-06-10T15:20:00+09:00",
                                  "phoneNumber": "01099998888",
                                  "victim": {
                                    "name": "김영희",
                                    "age": 71
                                  }
                                }
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String sessionId = JsonPath.read(sessionResponse, "$.sessionId");

        mockMvc.perform(post("/api/mobile/call-sessions/{sessionId}/transcripts/analyze", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chunkId": "chunk-rest-001",
                                  "sequence": 1,
                                  "text": "검찰입니다. 안전계좌로 바로 이체하세요.",
                                  "startedAtMs": 0,
                                  "endedAtMs": 5000,
                                  "isFinal": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedSequence").value(1))
                .andExpect(jsonPath("$.nextTranscriptSequence").value(2))
                .andExpect(jsonPath("$.riskScore").value(82))
                .andExpect(jsonPath("$.phishingType").value("ACCOUNT_TRANSFER_INDUCEMENT"))
                .andExpect(jsonPath("$.shouldOpenWebSocket").value(true));
    }
}
