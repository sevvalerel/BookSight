package com.booksight.booksight.controller;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.entity.Review;
import com.booksight.booksight.repository.UserRepository;
import com.booksight.booksight.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.booksight.booksight.dto.ReviewDTO;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public ReviewController(ReviewService reviewService, JwtUtil jwtUtil, UserRepository userRepository) {
        this.reviewService = reviewService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<Review> createReview(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        Long userId = getUserIdFromToken(authHeader);
        Long bookId = Long.valueOf(request.get("bookId").toString());
        String reviewText = request.get("reviewText").toString();
        Integer rating = Integer.valueOf(request.get("rating").toString());

        return ResponseEntity.ok(reviewService.createReview(userId, bookId, reviewText, rating));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Review> updateReview(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        Long userId = getUserIdFromToken(authHeader);
        String reviewText = request.get("reviewText").toString();
        Integer rating = Integer.valueOf(request.get("rating").toString());

        return ResponseEntity.ok(reviewService.updateReview(id, userId, reviewText, rating));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        reviewService.deleteReview(id, userId);
        return ResponseEntity.ok(Map.of("message", "Yorum silindi."));
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByBook(@PathVariable Long bookId) {
        List<Review> reviews = reviewService.getReviewsByBook(bookId);
        List<ReviewDTO> dtos = reviews.stream()
                .map(r -> new ReviewDTO(
                        r.getReviewId(),
                        r.getReviewText(),
                        r.getRating(),
                        r.getUser().getUsername(),
                        r.getCreatedAt().toString(),
                        r.getBook().getBookId(),
                        r.getBook().getTitle(),
                        r.getBook().getCoverUrl()
                ))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReviewDTO>> getMyReviews(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        List<Review> reviews = reviewService.getReviewsByUserWithBook(userId);
        List<ReviewDTO> dtos = reviews.stream()
                .map(r -> new ReviewDTO(
                        r.getReviewId(),
                        r.getReviewText(),
                        r.getRating(),
                        r.getUser().getUsername(),
                        r.getCreatedAt().toString(),
                        r.getBook().getBookId(),
                        r.getBook().getTitle(),
                        r.getBook().getCoverUrl()
                ))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"))
                .getUserId();
    }
}
