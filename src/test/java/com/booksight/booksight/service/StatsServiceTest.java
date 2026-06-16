package com.booksight.booksight.service;

import com.booksight.booksight.dto.StatsDTO;
import com.booksight.booksight.entity.Book;
import com.booksight.booksight.entity.NlpResult;
import com.booksight.booksight.entity.Review;
import com.booksight.booksight.repository.NlpResultRepository;
import com.booksight.booksight.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private NlpResultRepository nlpResultRepository;

    @InjectMocks
    private StatsService statsService;

    @Test
    void getStatsForUser_yorumYok_sifirStat() {
        when(reviewRepository.findByUserIdWithBook(1L)).thenReturn(List.of());

        StatsDTO stats = statsService.getStatsForUser(1L);

        assertEquals(0, stats.getTotalReviews());
        assertEquals(0.0, stats.getAverageRating());
        assertEquals(6, stats.getMonthlyTrend().size());
        assertTrue(stats.getGenreDistribution().isEmpty());
    }

    @Test
    void getStatsForUser_yorumlarVar_toplamVeOrtalamaDoğru() {
        Book book = new Book();
        book.setGenre("Roman");

        Review r1 = new Review();
        r1.setRating(4);
        r1.setCreatedAt(LocalDateTime.now());
        r1.setBook(book);

        Review r2 = new Review();
        r2.setRating(2);
        r2.setCreatedAt(LocalDateTime.now());
        r2.setBook(book);

        when(reviewRepository.findByUserIdWithBook(1L)).thenReturn(List.of(r1, r2));
        when(nlpResultRepository.findByReview(any())).thenReturn(Optional.empty());

        StatsDTO stats = statsService.getStatsForUser(1L);

        assertEquals(2, stats.getTotalReviews());
        assertEquals(3.0, stats.getAverageRating());
        assertEquals(2, stats.getReviewsThisMonth());
        assertEquals(1, stats.getGenreDistribution().size());
        assertEquals("Roman", stats.getGenreDistribution().get(0).getGenre());
    }

    @Test
    void getStatsForUser_nlpEtiketleriVar_topThemesHesaplaniyor() {
        Book book = new Book();

        Review r = new Review();
        r.setRating(5);
        r.setCreatedAt(LocalDateTime.now());
        r.setBook(book);

        NlpResult nlp = new NlpResult();
        nlp.setDetectedLabels(List.of("felsefi", "ask"));

        when(reviewRepository.findByUserIdWithBook(1L)).thenReturn(List.of(r));
        when(nlpResultRepository.findByReview(r)).thenReturn(Optional.of(nlp));

        StatsDTO stats = statsService.getStatsForUser(1L);

        assertFalse(stats.getTopThemes().isEmpty());
        assertTrue(stats.getTopThemes().contains("Felsefi") || stats.getTopThemes().contains("Aşk"));
    }

    @Test
    void getStatsForUser_aylikTrend_altAyDoner() {
        when(reviewRepository.findByUserIdWithBook(1L)).thenReturn(List.of());

        StatsDTO stats = statsService.getStatsForUser(1L);

        assertEquals(6, stats.getMonthlyTrend().size());
    }

    @Test
    void getStatsForUser_genreSizKitap_dagılımaDahilEdilmemeli() {
        Book book = new Book();

        Review r = new Review();
        r.setRating(3);
        r.setCreatedAt(LocalDateTime.now());
        r.setBook(book);

        when(reviewRepository.findByUserIdWithBook(1L)).thenReturn(List.of(r));
        when(nlpResultRepository.findByReview(any())).thenReturn(Optional.empty());

        StatsDTO stats = statsService.getStatsForUser(1L);

        assertTrue(stats.getGenreDistribution().isEmpty());
    }
}
