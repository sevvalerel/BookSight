package com.booksight.booksight.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NlpClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${nlp.service.url:http://localhost:8000}")
    private String nlpServiceUrl;

    @Data
    public static class AnalyzeRequest {
        private String review_id;
        private String text;
        public AnalyzeRequest(String reviewId, String text) {
            this.review_id = reviewId;
            this.text = text;
        }
    }

    @Data
    public static class LabelResult {
        private String label;
        private double confidence;
        private boolean detected;
    }

    @Data
    public static class AnalyzeResponse {
        private String review_id;
        private List<LabelResult> labels;
        @JsonProperty("detected_labels")
        private List<String> detectedLabels;
        private String processed_at;
    }

    public AnalyzeResponse analyze(String reviewId, String text) {
        log.info("NLP analiz isteği: reviewId={}", reviewId);
        try {
            return restTemplate.postForObject(
                    nlpServiceUrl + "/api/nlp/analyze",
                    new AnalyzeRequest(reviewId, text),
                    AnalyzeResponse.class
            );
        } catch (Exception e) {
            log.error("NLP servisi hatası: {}", e.getMessage());
            throw new RuntimeException("NLP servisi şu an yanıt vermiyor", e);
        }
    }

    public boolean isHealthy() {
        try {
            String response = restTemplate.getForObject(
                    nlpServiceUrl + "/api/nlp/health", String.class
            );
            return response != null && response.contains("ok");
        } catch (Exception e) {
            log.warn("NLP servisi erişilemiyor: {}", e.getMessage());
            return false;
        }
    }
}