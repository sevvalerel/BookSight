package com.booksight.booksight.service;

import com.booksight.booksight.entity.Book;
import com.booksight.booksight.entity.Review;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.BookRepository;
import com.booksight.booksight.repository.ReviewRepository;
import com.booksight.booksight.repository.UserRepository;
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
public class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private ReviewService reviewService;

    // --- Yardımcı nesneler ---

    private User createUser(Long id, String username) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        return user;
    }

    private Book createBook(Long id, String title) {
        Book book = new Book();
        book.setBookId(id);
        book.setTitle(title);
        return book;
    }

    private Review createReview(Long reviewId, User user, Book book, String text, int rating) {
        return Review.builder()
                .reviewId(reviewId)
                .user(user)
                .book(book)
                .reviewText(text)
                .rating(rating)
                .createdAt(LocalDateTime.now())
                .isAnalysisEnabled(true)
                .build();
    }

    // =====================================================================
    // createReview testleri
    // =====================================================================

    @Test
    void createReview_gecerliVeriler_yorumKaydedilmeli() {
        // ARRANGE
        String uzunYorum = "Bu kitap gerçekten çok etkileyici bir eserdi. Yazar karakterleri çok iyi işlemiş. " +
                "Okuması çok keyifli ve tavsiye ederim. Kesinlikle okunması gereken kitaplar arasında.";
        User user = createUser(1L, "sevval");
        Book book = createBook(10L, "1984");

        when(reviewRepository.existsByUserUserIdAndBookBookId(1L, 10L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));
        when(reviewRepository.findByBookBookIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());

        // ACT
        Review result = reviewService.createReview(1L, 10L, uzunYorum, 4);

        // ASSERT
        assertNotNull(result);
        assertEquals(uzunYorum, result.getReviewText());
        assertEquals(4, result.getRating());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_ayniKitabaIkinciYorum_hataDonmeli() {
        // ARRANGE
        when(reviewRepository.existsByUserUserIdAndBookBookId(1L, 10L)).thenReturn(true);

        // ACT & ASSERT
        assertThrows(RuntimeException.class, () ->
                reviewService.createReview(1L, 10L, "Yorum metni buraya gelecek yeterince uzun olmalı.", 3)
        );
    }

    @Test
    void createReview_cokKisaYorum_hataDonmeli() {
        // ARRANGE
        when(reviewRepository.existsByUserUserIdAndBookBookId(1L, 10L)).thenReturn(false);

        // ACT & ASSERT
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                reviewService.createReview(1L, 10L, "Kısa yorum", 3)
        );
        assertTrue(ex.getMessage().contains("50 ile 2000"));
    }

    @Test
    void createReview_gecersizPuan_hataDonmeli() {
        // ARRANGE
        String uzunYorum = "Bu kitap gerçekten çok etkileyici bir eserdi. Yazar karakterleri çok iyi işlemiş. " +
                "Kesinlikle okunması gereken kitaplar listesine alındı, tavsiye ederim herkese.";
        when(reviewRepository.existsByUserUserIdAndBookBookId(1L, 10L)).thenReturn(false);

        // ACT & ASSERT  (puan 0 — geçersiz)
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                reviewService.createReview(1L, 10L, uzunYorum, 0)
        );
        assertTrue(ex.getMessage().contains("1 ile 5"));
    }

    @Test
    void createReview_kullaniciBulunamazsa_hataDonmeli() {
        // ARRANGE
        String uzunYorum = "Bu kitap gerçekten çok etkileyici bir eserdi. Yazar karakterleri çok iyi işlemiş. " +
                "Kesinlikle okunması gereken kitaplar listesine alındı, tavsiye ederim herkese.";
        when(reviewRepository.existsByUserUserIdAndBookBookId(99L, 10L)).thenReturn(false);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(RuntimeException.class, () ->
                reviewService.createReview(99L, 10L, uzunYorum, 3)
        );
    }

    // =====================================================================
    // deleteReview testleri
    // =====================================================================

    @Test
    void deleteReview_yorumSahibi_silinmeli() {
        // ARRANGE
        User user = createUser(1L, "sevval");
        Book book = createBook(10L, "1984");
        Review review = createReview(5L, user, book, "Uzun bir yorum metni", 4);

        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));
        when(reviewRepository.findByBookBookIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());

        // ACT
        reviewService.deleteReview(5L, 1L);

        // ASSERT
        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_baskaKullanici_hataDonmeli() {
        // ARRANGE
        User asil = createUser(1L, "sevval");
        Book book = createBook(10L, "1984");
        Review review = createReview(5L, asil, book, "Uzun bir yorum metni", 4);

        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        // ACT & ASSERT — userId=2 sahibi değil
        assertThrows(RuntimeException.class, () ->
                reviewService.deleteReview(5L, 2L)
        );
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void deleteReview_olmayanYorum_hataDonmeli() {
        // ARRANGE
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(RuntimeException.class, () ->
                reviewService.deleteReview(999L, 1L)
        );
    }

    // =====================================================================
    // getReviewsByBook testleri
    // =====================================================================

    @Test
    void getReviewsByBook_kitapYorumlari_listeDonmeli() {
        // ARRANGE
        User user = createUser(1L, "sevval");
        Book book = createBook(10L, "1984");
        Review r1 = createReview(1L, user, book, "Birinci yorum metni buraya", 5);
        Review r2 = createReview(2L, user, book, "İkinci yorum metni buraya", 3);

        when(reviewRepository.findByBookBookIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(r1, r2));

        // ACT
        List<Review> result = reviewService.getReviewsByBook(10L);

        // ASSERT
        assertEquals(2, result.size());
        verify(reviewRepository).findByBookBookIdOrderByCreatedAtDesc(10L);
    }

    @Test
    void getReviewsByBook_yorumYok_bosListeDonmeli() {
        // ARRANGE
        when(reviewRepository.findByBookBookIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of());

        // ACT
        List<Review> result = reviewService.getReviewsByBook(10L);

        // ASSERT
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // =====================================================================
    // updateReview testleri
    // =====================================================================

    @Test
    void updateReview_gecerliVeriler_guncellenmeli() {
        // ARRANGE
        String yeniYorum = "Bu kitap gerçekten mükemmeldi, tekrar okurum. Yazar çok başarılı bir iş çıkarmış. " +
                "Karakterler gerçekçi ve akıcı bir dil kullanılmış, şiddetle tavsiye ederim.";
        User user = createUser(1L, "sevval");
        Book book = createBook(10L, "1984");
        Review review = createReview(5L, user, book, "Eski yorum metni uzunca yazılmış buraya", 3);

        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));
        when(reviewRepository.findByBookBookIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());

        // ACT
        Review result = reviewService.updateReview(5L, 1L, yeniYorum, 5);

        // ASSERT
        assertEquals(yeniYorum, result.getReviewText());
        assertEquals(5, result.getRating());
    }

    @Test
    void updateReview_baskaKullanici_hataDonmeli() {
        // ARRANGE
        User asil = createUser(1L, "sevval");
        Book book = createBook(10L, "1984");
        Review review = createReview(5L, asil, book, "Eski yorum metni uzunca yazılmış buraya", 3);

        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        // ACT & ASSERT — userId=2 sahibi değil
        assertThrows(RuntimeException.class, () ->
                reviewService.updateReview(5L, 2L, "Yeni metin yeterince uzun ve anlamlı olmalı.", 4)
        );
    }

    @Test
    void updateReview_cokKisaYorum_hataDonmeli() {
        // ARRANGE
        User user = createUser(1L, "sevval");
        Book book = createBook(10L, "1984");
        Review review = createReview(5L, user, book, "Eski yorum", 3);

        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        // ACT & ASSERT
        assertThrows(RuntimeException.class, () ->
                reviewService.updateReview(5L, 1L, "Kısa", 4)
        );
    }
    @Test
    void getReviewsByUser_yorumlarDonmeli() {
        User user = createUser(1L, "sevval");
        Book book = createBook(10L, "1984");
        Review r1 = createReview(1L, user, book, "Birinci yorum metni buraya yazıldı.", 5);

        when(reviewRepository.findByUserUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(r1));

        List<Review> result = reviewService.getReviewsByUser(1L);

        assertEquals(1, result.size());
        verify(reviewRepository).findByUserUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void getReviewsByUser_yorumYok_bosListeDonmeli() {
        when(reviewRepository.findByUserUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        List<Review> result = reviewService.getReviewsByUser(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void createReview_kitapBulunamazsa_hataDonmeli() {
        String uzunYorum = "Bu kitap gerçekten çok etkileyici bir eserdi. Yazar karakterleri çok iyi işlemiş. " +
                "Kesinlikle okunması gereken kitaplar listesine alındı, tavsiye ederim herkese.";
        User user = createUser(1L, "sevval");

        when(reviewRepository.existsByUserUserIdAndBookBookId(1L, 99L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                reviewService.createReview(1L, 99L, uzunYorum, 3)
        );
    }

    @Test
    void createReview_ratingBesten_buyuk_hataDonmeli() {
        String uzunYorum = "Bu kitap gerçekten çok etkileyici bir eserdi. Yazar karakterleri çok iyi işlemiş. " +
                "Kesinlikle okunması gereken kitaplar listesine alındı, tavsiye ederim herkese.";

        when(reviewRepository.existsByUserUserIdAndBookBookId(1L, 10L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                reviewService.createReview(1L, 10L, uzunYorum, 6)
        );
        assertTrue(ex.getMessage().contains("1 ile 5"));
    }

    @Test
    void updateReview_ratingBesten_buyuk_hataDonmeli() {
        User user = createUser(1L, "sevval");
        Book book = createBook(10L, "1984");
        Review review = createReview(5L, user, book, "Eski yorum metni uzunca yazılmış buraya", 3);
        String uzunYorum = "Bu kitap gerçekten çok etkileyici bir eserdi. Yazar karakterleri çok iyi işlemiş. " +
                "Kesinlikle okunması gereken kitaplar listesine alındı, tavsiye ederim herkese.";

        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        assertThrows(RuntimeException.class, () ->
                reviewService.updateReview(5L, 1L, uzunYorum, 6)
        );
    }

    @Test
    void updateReview_olmayanYorum_hataDonmeli() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                reviewService.updateReview(999L, 1L, "Yorum metni", 3)
        );
    }

    @Test
    void createReview_updateBookRating_yorumVarken_avgHesaplanmali() {
        String uzunYorum = "Bu kitap gerçekten çok etkileyici bir eserdi. Yazar karakterleri çok iyi işlemiş. " +
                "Kesinlikle okunması gereken kitaplar listesine alındı, tavsiye ederim herkese.";
        User user = createUser(1L, "sevval");
        Book book = createBook(10L, "1984");

        Review mevcutReview = createReview(1L, user, book, uzunYorum, 4);

        when(reviewRepository.existsByUserUserIdAndBookBookId(1L, 10L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));
        when(reviewRepository.findByBookBookIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(mevcutReview));

        reviewService.createReview(1L, 10L, uzunYorum, 4);

        assertEquals(4.0, book.getAvgRating());
        assertEquals(1, book.getReviewCount());
        verify(bookRepository).save(book);
    }
}
