package com.booksight.booksight.service;

import com.booksight.booksight.entity.Book;
import com.booksight.booksight.entity.Recommendation;
import com.booksight.booksight.entity.Review;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.BookRepository;
import com.booksight.booksight.repository.RecommendationRepository;
import com.booksight.booksight.repository.ReviewRepository;
import com.booksight.booksight.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public RecommendationService(RecommendationRepository recommendationRepository,
                                  ReviewRepository reviewRepository,
                                  BookRepository bookRepository,
                                  UserRepository userRepository) {
        this.recommendationRepository = recommendationRepository;
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    public List<Recommendation> getRecommendations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        // Kullanıcının daha önce yorum yaptığı kitapları al
        List<Review> userReviews = reviewRepository.findByUserUserId(userId);
        Set<Long> reviewedBookIds = userReviews.stream()
                .map(r -> r.getBook().getBookId())
                .collect(Collectors.toSet());

        // Kullanıcının en çok yorum yaptığı türü bul
        String preferredGenre = userReviews.stream()
                .filter(r -> r.getBook().getGenre() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getBook().getGenre(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // O türden, yorum yapılmamış, en yüksek puanlı kitapları getir
        List<Book> candidates;
        if (preferredGenre != null) {
            candidates = bookRepository.findByGenreContainingIgnoreCase(preferredGenre);
        } else {
            candidates = bookRepository.findAll();
        }

        List<Book> recommended = candidates.stream()
                .filter(b -> !reviewedBookIds.contains(b.getBookId()))
                .sorted(Comparator.comparingDouble(
                        b -> -(b.getAvgRating() != null ? b.getAvgRating() : 0.0)
                ))
                .limit(10)
                .collect(Collectors.toList());

        // Öneri kayıtlarını oluştur veya güncelle
        List<Recommendation> result = new ArrayList<>();
        for (Book book : recommended) {
            String reason = preferredGenre != null
                    ? "Okuma geçmişinize göre " + book.getGenre() + " türünü sevdiğiniz tespit edildi."
                    : "Yüksek puana sahip popüler bir kitap.";

            Recommendation rec = Recommendation.builder()
                    .user(user)
                    .book(book)
                    .reason(reason)
                    .confidenceScore(0.75)
                    .isAiGenerated(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            result.add(recommendationRepository.save(rec));
        }

        return result;
    }
}
