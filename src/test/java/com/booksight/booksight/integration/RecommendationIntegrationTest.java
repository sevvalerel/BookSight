package com.booksight.booksight.integration;

import com.booksight.booksight.service.EmailService;
import com.booksight.booksight.service.NlpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecommendationIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    EmailService emailService;

    @MockitoBean
    NlpClient nlpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String token;
    private Long bookId;

    @BeforeEach
    void setup() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

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

        Map<String, Object> bookBody = Map.of(
                "title", "Test Kitabı",
                "author", "Test Yazar",
                "isbn", "1234567890123",
                "description", "Test açıklama"
        );
        MvcResult bookResult = mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookBody)))
                .andReturn();

        Map<?, ?> bookResponse = objectMapper.readValue(
                bookResult.getResponse().getContentAsString(), Map.class);
        bookId = Long.valueOf(bookResponse.get("bookId").toString());
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

    @Test
    void feedbackGonder_basarili() throws Exception {
        Map<String, Boolean> feedbackBody = Map.of("liked", true);
        mockMvc.perform(post("/api/recommendations/" + bookId + "/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feedbackBody)))
                .andExpect(status().isOk());
    }
}
