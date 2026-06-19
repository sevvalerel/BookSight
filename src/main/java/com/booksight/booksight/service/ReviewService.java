package com.booksight.booksight.service;

import com.booksight.booksight.entity.Book;
import com.booksight.booksight.entity.Review;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.exception.ForbiddenException;
import com.booksight.booksight.exception.ResourceNotFoundException;
import com.booksight.booksight.repository.BookRepository;
import com.booksight.booksight.repository.ReviewRepository;
import com.booksight.booksight.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final RecommendationService recommendationService;

    public ReviewService(ReviewRepository reviewRepository,
                         BookRepository bookRepository,
                         UserRepository userRepository,
                         RecommendationService recommendationService) {
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.recommendationService = recommendationService;
    }
    public List<Review> getReviewsByUser(Long userId) {
        return reviewRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Review> getReviewsByUserWithBook(Long userId) {
        return reviewRepository.findByUserIdWithBook(userId);
    }

    public Review createReview(Long userId, Long bookId, String reviewText, Integer rating) {
        if (reviewRepository.existsByUserUserIdAndBookBookId(userId, bookId)) {
            throw new RuntimeException("Bu kitap için zaten yorum yazdınız!");
        }
        if (reviewText == null || reviewText.length() < 50 || reviewText.length() > 2000) {
            throw new RuntimeException("Yorum 50 ile 2000 karakter arasında olmalıdır!");
        }
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Puan 1 ile 5 arasında olmalıdır!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı!"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Kitap bulunamadı!"));

        Review review = Review.builder()
                .user(user)
                .book(book)
                .reviewText(reviewText)
                .rating(rating)
                .createdAt(LocalDateTime.now())
                .isAnalysisEnabled(true)
                .build();

        Review saved = reviewRepository.save(review);
        updateBookRating(book);
        if (Boolean.TRUE.equals(saved.getIsAnalysisEnabled())) {
            recommendationService.analyzeAndSave(saved);
        }
        return saved;
    }

    public Review updateReview(Long reviewId, Long userId, String reviewText, Integer rating) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Yorum bulunamadı!"));

        if (!review.getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("Bu yorumu düzenleme yetkiniz yok!");
        }
        if (reviewText == null || reviewText.length() < 50 || reviewText.length() > 2000) {
            throw new RuntimeException("Yorum 50 ile 2000 karakter arasında olmalıdır!");
        }
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Puan 1 ile 5 arasında olmalıdır!");
        }

        review.setReviewText(reviewText);
        review.setRating(rating);
        review.setUpdatedAt(LocalDateTime.now());

        Review saved = reviewRepository.save(review);
        updateBookRating(review.getBook());
        return saved;
    }

    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Yorum bulunamadı!"));

        if (!review.getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("Bu yorumu silme yetkiniz yok!");
        }

        Book book = review.getBook();
        reviewRepository.delete(review);
        updateBookRating(book);
    }

    public List<Review> getReviewsByBook(Long bookId) {
        return reviewRepository.findByBookBookIdOrderByCreatedAtDesc(bookId);
    }

    private void updateBookRating(Book book) {
        List<Review> reviews = reviewRepository.findByBookBookIdOrderByCreatedAtDesc(book.getBookId());
        if (reviews.isEmpty()) {
            book.setAvgRating(0.0);
            book.setReviewCount(0);
        } else {
            double avg = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
            book.setAvgRating(Math.round(avg * 10.0) / 10.0);
            book.setReviewCount(reviews.size());
        }
        bookRepository.save(book);
    }
}
