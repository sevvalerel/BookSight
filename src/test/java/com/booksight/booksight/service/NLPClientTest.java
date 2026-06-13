package com.booksight.booksight.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NlpClient Tests")
class NlpClientTest {

    private RestTemplate restTemplate;
    private NlpClient nlpClient;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        nlpClient = new NlpClient();
        ReflectionTestUtils.setField(nlpClient, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(nlpClient, "nlpServiceUrl", "http://localhost:8000");
    }

    @Nested
    @DisplayName("analyze()")
    class Analyze {

        @Test
        @DisplayName("Başarılı istek AnalyzeResponse döner")
        void shouldReturnAnalyzeResponseOnSuccess() {
            NlpClient.AnalyzeResponse mockResponse = new NlpClient.AnalyzeResponse();
            mockResponse.setReview_id("42");
            mockResponse.setDetectedLabels(java.util.List.of("adventure"));

            when(restTemplate.postForObject(
                    eq("http://localhost:8000/api/nlp/analyze"),
                    any(NlpClient.AnalyzeRequest.class),
                    eq(NlpClient.AnalyzeResponse.class)
            )).thenReturn(mockResponse);

            NlpClient.AnalyzeResponse result = nlpClient.analyze("42", "Harika bir kitap.");

            assertThat(result).isNotNull();
            assertThat(result.getReview_id()).isEqualTo("42");
            assertThat(result.getDetectedLabels()).contains("adventure");
        }

        @Test
        @DisplayName("RestTemplate hata fırlatırsa RuntimeException sarılır")
        void shouldThrowRuntimeExceptionWhenRestTemplateFails() {
            when(restTemplate.postForObject(anyString(), any(), any()))
                    .thenThrow(new RuntimeException("Bağlantı hatası"));

            assertThatThrownBy(() -> nlpClient.analyze("1", "test"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("NLP servisi şu an yanıt vermiyor");
        }
    }

    @Nested
    @DisplayName("isHealthy()")
    class IsHealthy {

        @Test
        @DisplayName("Servis ok döndürürse true döner")
        void shouldReturnTrueWhenServiceIsHealthy() {
            when(restTemplate.getForObject(
                    "http://localhost:8000/api/nlp/health", String.class
            )).thenReturn("{\"status\": \"ok\"}");

            assertThat(nlpClient.isHealthy()).isTrue();
        }

        @Test
        @DisplayName("Servis ok içermeyen yanıt döndürürse false döner")
        void shouldReturnFalseWhenResponseDoesNotContainOk() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn("{\"status\": \"error\"}");

            assertThat(nlpClient.isHealthy()).isFalse();
        }

        @Test
        @DisplayName("Servis null döndürürse false döner")
        void shouldReturnFalseWhenResponseIsNull() {
            when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

            assertThat(nlpClient.isHealthy()).isFalse();
        }

        @Test
        @DisplayName("Servis erişilemezse false döner")
        void shouldReturnFalseWhenServiceIsUnreachable() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenThrow(new RuntimeException("Bağlantı reddedildi"));

            assertThat(nlpClient.isHealthy()).isFalse();
        }
    }
}