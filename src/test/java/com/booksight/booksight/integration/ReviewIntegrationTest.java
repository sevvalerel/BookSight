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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EmailService emailService;

    @MockitoBean
    NlpClient nlpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String token;
    private Long bookId;

    @BeforeEach
    void setup() throws Exception {
        Map<String, String> registerBody = Map.of(
                "username", "reviewuser",
                "email", "reviewuser@test.com",
                "password", "Test1234!"
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerBody)));

        Map<String, String> loginBody = Map.of(
                "email", "reviewuser@test.com",
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
    void yorumEkle_basariliKaydedilmeli() throws Exception {
        Map<String, Object> reviewBody = Map.of(
                "bookId", bookId,
                "reviewText", "Bu kitap gerçekten çok etkileyiciydi, karakterler inanılmaz derecede iyi işlenmiş ve hikaye sonu beklenmedik bir şekilde gelişti.",
                "rating", 5
        );
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewBody)))
                .andExpect(status().isOk());
    }

    @Test
    void yorumEkle_tokenSiz_reddedilmeli() throws Exception {
        Map<String, Object> reviewBody = Map.of(
                "bookId", bookId,
                "reviewText", "Token olmadan yorum yazılamaz bu yüzden bu istek reddedilmeli.",
                "rating", 3
        );
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewBody)))
                .andExpect(status().is4xxClientError());
    }
}