package com.booksight.booksight.controller;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.entity.*;
import com.booksight.booksight.repository.NlpResultRepository;
import com.booksight.booksight.repository.ReviewRepository;
import com.booksight.booksight.repository.UserRepository;
import com.booksight.booksight.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationController Tests")
class RecommendationControllerTest {

    @Mock private RecommendationService recommendationService;
    @Mock private JwtUtil jwtUtil;
    @Mock private UserRepository userRepository;
    @Mock private NlpResultRepository nlpResultRepository;
    @Mock private ReviewRepository reviewRepository;

    @InjectMocks
    private RecommendationController recommendationController;

    private User user;
    private Book book;
    private Recommendation recommendation;
    private Review review;
    private static final String AUTH_HEADER = "Bearer test-token";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setUsername("testuser");

        book = new Book();
        book.setBookId(10L);
        book.setTitle("Test Kitap");
        book.setAuthor("Test Yazar");
        book.setCoverUrl("http://cover.jpg");
        book.setGenre("Roman");
        book.setLabels(List.of("macera", "gizem_polisiye"));

        recommendation = new Recommendation();
        recommendation.setUser(user);
        recommendation.setBook(book);
        recommendation.setConfidenceScore(0.85);
        recommendation.setIsAiGenerated(true);

        review = new Review();
        review.setReviewId(100L);
        review.setUser(user);
        review.setBook(book);

        when(jwtUtil.extractUsername("test-token")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    }

    @Nested
    @DisplayName("GET /api/recommendations")
    class GetRecommendations {

        @Test
        @DisplayName("200 ve recommendations + analyzedReviews döner")
        void shouldReturn200WithRecommendationsAndAnalyzedReviews() {
            when(recommendationService.getRecommendations(1L)).thenReturn(List.of(recommendation));
            when(reviewRepository.findByUserUserId(1L)).thenReturn(List.of(review));
            when(nlpResultRepository.findByReview(review)).thenReturn(Optional.of(new NlpResult()));

            ResponseEntity<Map<String, Object>> response =
                    recommendationController.getRecommendations(AUTH_HEADER);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsKeys("recommendations", "analyzedReviews");
        }

        @Test
        @DisplayName("Öneri listesi doğru boyutta döner")
        void shouldReturnCorrectNumberOfRecommendations() {
            when(recommendationService.getRecommendations(1L)).thenReturn(List.of(recommendation));
            when(reviewRepository.findByUserUserId(1L)).thenReturn(List.of());

            ResponseEntity<Map<String, Object>> response =
                    recommendationController.getRecommendations(AUTH_HEADER);

            List<?> recommendations = (List<?>) response.getBody().get("recommendations");
            assertThat(recommendations).hasSize(1);
        }

        @Test
        @DisplayName("analyzedReviews NLP sonucu olan yorumları sayar")
        void shouldCountOnlyAnalyzedReviews() {
            Review review2 = new Review();
            review2.setReviewId(101L);

            when(recommendationService.getRecommendations(1L)).thenReturn(List.of());
            when(reviewRepository.findByUserUserId(1L)).thenReturn(List.of(review, review2));
            when(nlpResultRepository.findByReview(review)).thenReturn(Optional.of(new NlpResult()));
            when(nlpResultRepository.findByReview(review2)).thenReturn(Optional.empty());

            ResponseEntity<Map<String, Object>> response =
                    recommendationController.getRecommendations(AUTH_HEADER);

            assertThat(response.getBody().get("analyzedReviews")).isEqualTo(1L);
        }

        @Test
        @DisplayName("Kitabın labels'ı varsa reason etikete göre oluşturulur")
        void shouldBuildReasonFromLabels() {
            when(recommendationService.getRecommendations(1L)).thenReturn(List.of(recommendation));
            when(reviewRepository.findByUserUserId(1L)).thenReturn(List.of());

            ResponseEntity<Map<String, Object>> response =
                    recommendationController.getRecommendations(AUTH_HEADER);

            List<?> recommendations = (List<?>) response.getBody().get("recommendations");
            assertThat(recommendations).isNotEmpty();
        }

        @Test
        @DisplayName("Kitabın labels'ı boşsa reason 'Popüler kitaplar arasında' olur")
        void shouldUsePopularReasonWhenLabelsEmpty() {
            book.setLabels(List.of());
            when(recommendationService.getRecommendations(1L)).thenReturn(List.of(recommendation));
            when(reviewRepository.findByUserUserId(1L)).thenReturn(List.of());

            ResponseEntity<Map<String, Object>> response =
                    recommendationController.getRecommendations(AUTH_HEADER);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }

        @Test
        @DisplayName("Kitabın labels'ı null ise hata fırlatmaz")
        void shouldHandleNullLabels() {
            book.setLabels(null);
            when(recommendationService.getRecommendations(1L)).thenReturn(List.of(recommendation));
            when(reviewRepository.findByUserUserId(1L)).thenReturn(List.of());

            assertThatNoException().isThrownBy(() ->
                    recommendationController.getRecommendations(AUTH_HEADER));
        }

        @Test
        @DisplayName("confidenceScore null ise 0.0 olarak set edilir")
        void shouldDefaultConfidenceScoreToZeroWhenNull() {
            recommendation.setConfidenceScore(null);
            when(recommendationService.getRecommendations(1L)).thenReturn(List.of(recommendation));
            when(reviewRepository.findByUserUserId(1L)).thenReturn(List.of());

            assertThatNoException().isThrownBy(() ->
                    recommendationController.getRecommendations(AUTH_HEADER));
        }

        @Test
        @DisplayName("Olmayan kullanıcı için RuntimeException fırlatır")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recommendationController.getRecommendations(AUTH_HEADER))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Kullanıcı bulunamadı");
        }

        @Test
        @DisplayName("Öneri listesi boşsa boş liste döner")
        void shouldReturnEmptyListWhenNoRecommendations() {
            when(recommendationService.getRecommendations(1L)).thenReturn(List.of());
            when(reviewRepository.findByUserUserId(1L)).thenReturn(List.of());

            ResponseEntity<Map<String, Object>> response =
                    recommendationController.getRecommendations(AUTH_HEADER);

            List<?> recommendations = (List<?>) response.getBody().get("recommendations");
            assertThat(recommendations).isEmpty();
        }
    }
}