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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReadingStatusIntegrationTest {

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

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "username", "statususer",
                        "email", "statususer@test.com",
                        "password", "Test1234!"
                ))));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "statususer@test.com",
                                "password", "Test1234!"
                        ))))
                .andReturn();

        token = (String) objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), Map.class).get("token");

        MvcResult bookResult = mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Status Test Kitabı",
                                "author", "Test Yazar",
                                "isbn", "9876543210123",
                                "description", "Test açıklama"
                        ))))
                .andReturn();

        bookId = Long.valueOf(objectMapper.readValue(
                bookResult.getResponse().getContentAsString(), Map.class)
                .get("bookId").toString());
    }

    @Test
    void okumaDurumuEkle_basarili() throws Exception {
        mockMvc.perform(post("/api/reading-status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "bookId", bookId,
                                "status", "READING"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READING"));
    }

    @Test
    void okumaDurumuGuncelle_basarili() throws Exception {
        mockMvc.perform(post("/api/reading-status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "bookId", bookId,
                                "status", "READING"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/reading-status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "bookId", bookId,
                                "status", "READ"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"));
    }

    @Test
    void kutuphanemiListele_basarili() throws Exception {
        mockMvc.perform(post("/api/reading-status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "bookId", bookId,
                                "status", "WILL_READ"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reading-status/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void kutuphanedenkitabiSil_basarili() throws Exception {
        mockMvc.perform(post("/api/reading-status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "bookId", bookId,
                                "status", "READING"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/reading-status/" + bookId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void okumaDurumu_tokensiz_reddedilmeli() throws Exception {
        mockMvc.perform(post("/api/reading-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "bookId", bookId,
                                "status", "READING"
                        ))))
                .andExpect(status().is4xxClientError());
    }
}
