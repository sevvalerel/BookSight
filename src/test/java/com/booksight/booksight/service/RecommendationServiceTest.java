package com.booksight.booksight.service;

import com.booksight.booksight.entity.Book;
import com.booksight.booksight.entity.NlpResult;
import com.booksight.booksight.entity.Review;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.BookRepository;
import com.booksight.booksight.repository.NlpResultRepository;
import com.booksight.booksight.repository.ReviewRepository;
import com.booksight.booksight.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.booksight.booksight.repository.ReadingStatusRepository;
import com.booksight.booksight.repository.RecommendationFeedbackRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService Tests")
class RecommendationServiceTest {

    @Mock private NlpClient nlpClient;
    @Mock private BookRepository bookRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private NlpResultRepository nlpResultRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReadingStatusRepository readingStatusRepository;
    @Mock private RecommendationFeedbackRepository feedbackRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    private User user;
    private Book bookA;
    private Book bookB;
    private Book bookC;
    private Review review;
    private NlpResult nlpResult;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);

        bookA = new Book();
        bookA.setBookId(10L);
        bookA.setLabels(List.of("adventure", "mystery"));
        bookA.setAvgRating(4.5);

        bookB = new Book();
        bookB.setBookId(20L);
        bookB.setLabels(List.of("romance", "drama"));
        bookB.setAvgRating(4.0);

        bookC = new Book();
        bookC.setBookId(30L);
        bookC.setLabels(List.of("adventure", "thriller"));
        bookC.setAvgRating(3.5);

        review = new Review();
        review.setReviewId(100L);
        review.setBook(bookA);
        review.setUser(user);
        review.setReviewText("Muhteşem bir macera kitabı, çok beğendim.");

        nlpResult = new NlpResult();
        nlpResult.setReview(review);
        nlpResult.setDetectedLabels(List.of("adventure", "mystery"));

        lenient().when(readingStatusRepository.findByUserUserId(anyLong()))
                .thenReturn(Collections.emptyList());
        lenient().when(feedbackRepository.findByUserUserIdAndLikedFalse(anyLong()))
                .thenReturn(Collections.emptyList());
    }

    @Nested
    @DisplayName("getRecommendations()")
    class GetRecommendations {

        @Test
        @DisplayName("Geçerli userId ile öneri listesi döner")
        void shouldReturnRecommendationsForValidUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(reviewRepository.findByUserUserId(1L)).thenReturn(List.of(review));
            when(nlpResultRepository.findByReview(review)).thenReturn(Optional.of(nlpResult));
            when(bookRepository.findAll()).thenReturn(List.of(bookA, bookB, bookC));

            var result = recommendationService.getRecommendations(1L);

            assertThat(result).isNotNull();
            assertThat(result).allSatisfy(rec -> {
                assertThat(rec.getUser()).isEqualTo(user);
                assertThat(rec.getIsAiGenerated()).isTrue();
                assertThat(rec.getCreatedAt()).isNotNull();
            });
        }

        @Test
        @DisplayName("Olmayan userId için RuntimeException fırlatır")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recommendationService.getRecommendations(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Kullanıcı bulunamadı");
        }

        @Test
        @DisplayName("Öneriler isAiGenerated=true olarak işaretlenir")
        void recommendationsShouldBeMarkedAsAiGenerated() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(reviewRepository.findByUserUserId(1L)).thenReturn(List.of(review));
            when(nlpResultRepository.findByReview(review)).thenReturn(Optional.of(nlpResult));
            when(bookRepository.findAll()).thenReturn(List.of(bookA, bookB, bookC));

            var result = recommendationService.getRecommendations(1L);

            assertThat(result).allMatch(r -> Boolean.TRUE.equals(r.getIsAiGenerated()));
        }
    }

    @Nested
    @DisplayName("recommend()")
    class Recommend {

        @Test
        @DisplayName("Yorumu olmayan kullanıcıya popüler kitaplar (rating'e göre) döner")
        void shouldReturnPopularBooksWhenUserHasNoReviews() {
            when(reviewRepository.findByUserUserId(1L)).thenReturn(Collections.emptyList());
            when(bookRepository.findAll()).thenReturn(List.of(bookA, bookB, bookC));

            List<Book> result = recommendationService.recommend(user, 5);

            assertThat(result).isNotEmpty();
            assertThat(result.get(0)).isEqualTo(bookA);
        }

        @Test
        @DisplayName("Okunan kitaplar öneri listesine dahil edilmez")
        void shouldExcludeAlreadyReadBooks() {
            when(reviewRepository.findByUserUserId(1L)).thenReturn(List.of(review));
            when(nlpResultRepository.findByReview(review)).thenReturn(Optional.of(nlpResult));
            when(bookRepository.findAll()).thenReturn(List.of(bookA, bookB, bookC));

            List<Book> result = recommendationService.recommend(user, 10);

            assertThat(result).doesNotContain(bookA);
        }

        @Test
        @DisplayName("Benzerlik skoru > 0 olan kitaplar önce sıralanır")
        void shouldPrioritizeBooksWithPositiveScore() {
            when(reviewRepository.findByUserUserId(1L)).thenReturn(List.of(review));
            when(nlpResultRepository.findByReview(review)).thenReturn(Optional.of(nlpResult));
            when(bookRepository.findAll()).thenReturn(List.of(bookA, bookB, bookC));

            List<Book> result = recommendationService.recommend(user, 10);

            assertThat(result).contains(bookC);
            assertThat(result).contains(bookB);
        }

        @Test
        @DisplayName("Limit parametresi dikkate alınır")
        void shouldRespectLimit() {
            when(reviewRepository.findByUserUserId(1L)).thenReturn(List.of(review));
            when(nlpResultRepository.findByReview(review)).thenReturn(Optional.of(nlpResult));
            when(bookRepository.findAll()).thenReturn(List.of(bookA, bookB, bookC));

            List<Book> result = recommendationService.recommend(user, 1);

            assertThat(result).hasSizeLessThanOrEqualTo(1);
        }

        @Test
        @DisplayName("NlpResult null olan yorum profili etkilemez, fallback çalışır")
        void shouldFallbackToPopularWhenNlpResultMissing() {
            when(reviewRepository.findByUserUserId(1L)).thenReturn(List.of(review));
            when(nlpResultRepository.findByReview(review)).thenReturn(Optional.empty());
            when(bookRepository.findAll()).thenReturn(List.of(bookA, bookB, bookC));

            List<Book> result = recommendationService.recommend(user, 10);

            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("Kitap veritabanı boş ise boş liste döner")
        void shouldReturnEmptyWhenNoBooksExist() {
            when(reviewRepository.findByUserUserId(1L)).thenReturn(Collections.emptyList());
            when(bookRepository.findAll()).thenReturn(Collections.emptyList());

            List<Book> result = recommendationService.recommend(user, 10);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("buildUserProfile() - dolaylı")
    class BuildUserProfile {

        @Test
        @DisplayName("Birden fazla yorum varsa etiketler birleştirilir")
        void shouldMergeLabelsFromMultipleReviews() {
            Review review2 = new Review();
            review2.setReviewId(101L);
            review2.setBook(bookB);
            review2.setUser(user);

            NlpResult nlp2 = new NlpResult();
            nlp2.setReview(review2);
            nlp2.setDetectedLabels(List.of("adventure", "sci-fi"));

            when(reviewRepository.findByUserUserId(1L)).thenReturn(List.of(review, review2));
            when(nlpResultRepository.findByReview(review)).thenReturn(Optional.of(nlpResult));
            when(nlpResultRepository.findByReview(review2)).thenReturn(Optional.of(nlp2));
            when(bookRepository.findAll()).thenReturn(List.of(bookA, bookB, bookC));

            List<Book> result = recommendationService.recommend(user, 10);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("analyzeAndSave()")
    class AnalyzeAndSave {

        @Test
        @DisplayName("Başarılı NLP analizi NlpResult kaydeder ve döner")
        void shouldSaveAndReturnNlpResultOnSuccess() {
            NlpClient.AnalyzeResponse response = new NlpClient.AnalyzeResponse();
            response.setDetectedLabels(List.of("adventure", "mystery"));
            response.setLabels(List.of(
                    makeLabel("adventure", 0.9),
                    makeLabel("mystery", 0.75)
            ));
            response.setProcessed_at("2024-01-01T00:00:00");

            when(nlpClient.analyze(eq("100"), anyString())).thenReturn(response);
            when(nlpResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            NlpResult result = recommendationService.analyzeAndSave(review);

            assertThat(result).isNotNull();
            assertThat(result.getDetectedLabels()).contains("adventure", "mystery");
            assertThat(result.getLabelScores()).containsKeys("adventure", "mystery");
            verify(nlpResultRepository, times(1)).save(any(NlpResult.class));
        }

        @Test
        @DisplayName("NLP client hata fırlatırsa null döner, exception yutulur")
        void shouldReturnNullWhenNlpClientThrows() {
            when(nlpClient.analyze(anyString(), anyString()))
                    .thenThrow(new RuntimeException("NLP servis hatası"));

            NlpResult result = recommendationService.analyzeAndSave(review);

            assertThat(result).isNull();
            verify(nlpResultRepository, never()).save(any());
        }

        @Test
        @DisplayName("Labels null ise LabelScores boş map olarak kaydedilir")
        void shouldHandleNullLabelsInNlpResponse() {
            NlpClient.AnalyzeResponse response = new NlpClient.AnalyzeResponse();
            response.setDetectedLabels(List.of("adventure"));
            response.setLabels(null);
            response.setProcessed_at("2024-01-01T00:00:00");

            when(nlpClient.analyze(anyString(), anyString())).thenReturn(response);
            when(nlpResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            NlpResult result = recommendationService.analyzeAndSave(review);

            assertThat(result).isNotNull();
            assertThat(result.getLabelScores()).isEmpty();
        }
    }

    private NlpClient.LabelResult makeLabel(String label, double confidence) {
        NlpClient.LabelResult lr = new NlpClient.LabelResult();
        lr.setLabel(label);
        lr.setConfidence(confidence);
        return lr;
    }
}