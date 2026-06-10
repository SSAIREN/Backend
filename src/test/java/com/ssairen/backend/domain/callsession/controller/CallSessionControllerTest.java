package com.ssairen.backend.domain.callsession.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CallSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void Flutter_세션_생성_요청을_본문_기반_계약으로_받는다() throws Exception {
        mockMvc.perform(post("/api/mobile/call-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalCallId": "device-call-controller-test",
                                  "deviceId": "victim-device-001",
                                  "startedAt": "2026-06-10T15:20:00+09:00",
                                  "phoneNumber": "01012345678",
                                  "victim": {
                                    "name": "김OO",
                                    "age": 71
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.nextTranscriptSequence").value(1))
                .andExpect(jsonPath("$.webSocketUrl").value(org.hamcrest.Matchers.containsString("/ws/v1/victim?sessionId=")));
    }
}
