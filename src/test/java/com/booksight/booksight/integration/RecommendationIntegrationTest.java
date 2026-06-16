package com.booksight.booksight.integration;

import com.booksight.booksight.service.EmailService;
import com.booksight.booksight.service.NlpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecommendationIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EmailService emailService;

    @MockitoBean
    NlpClient nlpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String token;

    @BeforeEach
    void setup() throws Exception {
        Map<String, String> registerBody = Map.of(
                "username", "recuser",
                "email", "recuser@test.com",
                "password", "Test1234!"
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerBody)));

        Map<String, String> loginBody = Map.of(
                "email", "recuser@test.com",
                "password", "Test1234!"
        );
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andReturn();

        Map<?, ?> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), Map.class);
        token = (String) response.get("token");
    }

    @Test
    void oneriAl_tokenileBasarili() throws Exception {
        mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations").exists());
    }

    @Test
    void oneriAl_tokensiz_reddedilmeli() throws Exception {
        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().is4xxClientError());
    }
}