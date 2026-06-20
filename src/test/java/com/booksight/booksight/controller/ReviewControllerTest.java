package com.booksight.booksight.controller;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.entity.Book;
import com.booksight.booksight.entity.Review;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.UserRepository;
import com.booksight.booksight.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewController reviewController;

    private User makeUser(Long id, String username) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        return user;
    }

    private Book makeBook(Long id, String title) {
        Book book = new Book();
        book.setBookId(id);
        book.setTitle(title);
        return book;
    }

    private Review makeReview(Long id, User user, Book book, String text, int rating) {
        return Review.builder()
                .reviewId(id)
                .user(user)
                .book(book)
                .reviewText(text)
                .rating(rating)
                .createdAt(LocalDateTime.now())
                .isAnalysisEnabled(true)
                .build();
    }

    private void mockToken(String username, Long userId) {
        when(jwtUtil.extractUsername("mock.token")).thenReturn(username);
        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(makeUser(userId, username)));
    }

    @Test
    void createReview_basarili_yorumDonmeli() {
        mockToken("sevval", 1L);
        User user = makeUser(1L, "sevval");
        Book book = makeBook(10L, "1984");
        Review review = makeReview(1L, user, book, "Uzun bir yorum metni burada yer alıyor.", 4);

        when(reviewService.createReview(1L, 10L, "Uzun bir yorum metni burada yer alıyor.", 4))
                .thenReturn(review);

        Map<String, Object> body = Map.of(
                "bookId", "10",
                "reviewText", "Uzun bir yorum metni burada yer alıyor.",
                "rating", "4"
        );

        ResponseEntity<Review> response = reviewController.createReview("Bearer mock.token", body);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(4, response.getBody().getRating());
    }

    @Test
    void createReview_kullaniciBulunamazsa_exceptionFirlatmali() {
        when(jwtUtil.extractUsername("mock.token")).thenReturn("yok");
        when(userRepository.findByUsername("yok")).thenReturn(Optional.empty());

        Map<String, Object> body = Map.of(
                "bookId", "10",
                "reviewText", "Uzun bir yorum metni.",
                "rating", "4"
        );

        assertThrows(RuntimeException.class, () ->
                reviewController.createReview("Bearer mock.token", body));
    }

    @Test
    void updateReview_basarili_guncellenmeli() {
        mockToken("sevval", 1L);
        User user = makeUser(1L, "sevval");
        Book book = makeBook(10L, "1984");
        Review updated = makeReview(5L, user, book, "Güncellenmiş yorum metni buraya yazıldı.", 5);

        when(reviewService.updateReview(5L, 1L, "Güncellenmiş yorum metni buraya yazıldı.", 5))
                .thenReturn(updated);

        Map<String, Object> body = Map.of(
                "reviewText", "Güncellenmiş yorum metni buraya yazıldı.",
                "rating", "5"
        );

        ResponseEntity<Review> response = reviewController.updateReview(5L, "Bearer mock.token", body);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(5, response.getBody().getRating());
    }

    @Test
    void updateReview_yetkisizKullanici_exceptionFirlatmali() {
        mockToken("sevval", 2L);
        when(reviewService.updateReview(eq(5L), eq(2L), anyString(), anyInt()))
                .thenThrow(new RuntimeException("Bu yorumu düzenleme yetkiniz yok!"));

        Map<String, Object> body = Map.of(
                "reviewText", "Güncellenmiş yorum metni buraya yazıldı.",
                "rating", "5"
        );

        assertThrows(RuntimeException.class, () ->
                reviewController.updateReview(5L, "Bearer mock.token", body));
    }

    @Test
    void deleteReview_basarili_mesajDonmeli() {
        mockToken("sevval", 1L);
        doNothing().when(reviewService).deleteReview(5L, 1L);

        ResponseEntity<?> response = reviewController.deleteReview(5L, "Bearer mock.token");

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("Yorum silindi.", responseBody.get("message"));
        verify(reviewService).deleteReview(5L, 1L);
    }

    @Test
    void deleteReview_yetkisizKullanici_exceptionFirlatmali() {
        mockToken("sevval", 2L);
        doThrow(new RuntimeException("Bu yorumu silme yetkiniz yok!"))
                .when(reviewService).deleteReview(5L, 2L);

        assertThrows(RuntimeException.class, () ->
                reviewController.deleteReview(5L, "Bearer mock.token"));
    }

    @Test
    void getReviewsByBook_yorumlarDonmeli() {
        User user = makeUser(1L, "sevval");
        Book book = makeBook(10L, "1984");
        Review r1 = makeReview(1L, user, book, "Birinci yorum metni buraya yazıldı.", 5);
        Review r2 = makeReview(2L, user, book, "İkinci yorum metni buraya yazıldı.", 3);

        when(reviewService.getReviewsByBook(10L, "date_desc")).thenReturn(List.of(r1, r2));

        ResponseEntity<?> response = reviewController.getReviewsByBook(10L, "date_desc");

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getReviewsByBook_bosListe_donmeli() {
        when(reviewService.getReviewsByBook(99L, "date_desc")).thenReturn(List.of());

        ResponseEntity<?> response = reviewController.getReviewsByBook(99L, "date_desc");

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getMyReviews_basarili_yorumlarDonmeli() {
        mockToken("sevval", 1L);
        User user = makeUser(1L, "sevval");
        Book book = makeBook(10L, "1984");
        Review r1 = makeReview(1L, user, book, "Benim yorumum burada yer alıyor.", 4);

        when(reviewService.getReviewsByUserWithBook(1L)).thenReturn(List.of(r1));

        ResponseEntity<?> response = reviewController.getMyReviews("Bearer mock.token");

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getMyReviews_yorumYok_bosListeDonmeli() {
        mockToken("sevval", 1L);
        when(reviewService.getReviewsByUserWithBook(1L)).thenReturn(List.of());

        ResponseEntity<?> response = reviewController.getMyReviews("Bearer mock.token");

        assertEquals(200, response.getStatusCode().value());
    }
}