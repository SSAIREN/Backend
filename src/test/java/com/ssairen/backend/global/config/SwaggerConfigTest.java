package com.ssairen.backend.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SwaggerConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void Flutter_REST와_WebSocket_계약이_OpenAPI_문서에_포함된다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("SSAIREN Flutter - Spring Boot API"))
                .andExpect(jsonPath("$.paths['/api/mobile/call-sessions'].post").exists())
                .andExpect(jsonPath("$.paths['/api/mobile/call-sessions/{sessionId}'].get").exists())
                .andExpect(jsonPath("$.paths['/ws/v1/victim'].get.responses['101']").exists());
    }
}
