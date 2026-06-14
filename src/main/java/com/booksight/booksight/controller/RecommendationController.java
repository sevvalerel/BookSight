package com.booksight.booksight.controller;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.dto.RecommendationDTO;
import com.booksight.booksight.entity.RecommendationFeedback;
import com.booksight.booksight.repository.*;
import com.booksight.booksight.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.booksight.booksight.repository.RecommendationRepository;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final NlpResultRepository nlpResultRepository;
    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final RecommendationFeedbackRepository feedbackRepository;
    private final RecommendationRepository recommendationRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getRecommendations(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"))
                .getUserId();

        var recommendations = recommendationService.getRecommendations(userId);

        List<RecommendationDTO> result = recommendations.stream().map(rec -> {
            var book = rec.getBook();
            List<String> detectedLabels = book.getLabels() != null ? book.getLabels() : List.of();

            String reason = detectedLabels.isEmpty()
                    ? "Popüler kitaplar arasında"
                    : turkishLabel(detectedLabels.get(0)) + " içeren kitapları seviyorsun";

            return RecommendationDTO.builder()
                    .bookId(book.getBookId())
                    .title(book.getTitle())
                    .author(book.getAuthor())
                    .coverUrl(book.getCoverUrl())
                    .genre(book.getGenre())
                    .detectedLabels(detectedLabels)
                    .confidenceScore(rec.getConfidenceScore() != null ? rec.getConfidenceScore() : 0.0)
                    .reason(reason)
                    .build();
        }).toList();

        long analyzedReviews = reviewRepository.findByUserUserId(userId).stream()
                .filter(r -> nlpResultRepository.findByReview(r).isPresent())
                .count();
        for (int i = 0; i < recommendations.size() && i < result.size(); i++) {
            recommendations.get(i).setReason(result.get(i).getReason());
        }
        recommendationRepository.saveAll(recommendations);

        return ResponseEntity.ok(Map.of("recommendations", result, "analyzedReviews", analyzedReviews));
    }
    @GetMapping("/history")
    public ResponseEntity<List<RecommendationDTO>> getHistory(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"))
                .getUserId();

        List<RecommendationDTO> history = recommendationRepository
                .findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(rec -> RecommendationDTO.builder()
                        .bookId(rec.getBook().getBookId())
                        .title(rec.getBook().getTitle())
                        .author(rec.getBook().getAuthor())
                        .coverUrl(rec.getBook().getCoverUrl())
                        .genre(rec.getBook().getGenre())
                        .reason(rec.getReason() != null ? rec.getReason() : "")
                        .confidenceScore(rec.getConfidenceScore() != null ? rec.getConfidenceScore() : 0.0)
                        .detectedLabels(rec.getBook().getLabels() != null ? rec.getBook().getLabels() : List.of())
                        .build())
                .toList();

        return ResponseEntity.ok(history);
    }

    @PostMapping("/{bookId}/feedback")
    public ResponseEntity<Map<String, String>> submitFeedback(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long bookId,
            @RequestBody Map<String, Boolean> body) {

        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));
        var book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Kitap bulunamadı!"));

        boolean liked = body.getOrDefault("liked", true);

        // Daha önce feedback varsa güncelle, yoksa yeni oluştur
        var existing = feedbackRepository.findByUserUserIdAndBookBookId(user.getUserId(), bookId);
        if (existing.isPresent()) {
            existing.get().setLiked(liked);
            feedbackRepository.save(existing.get());
        } else {
            feedbackRepository.save(RecommendationFeedback.builder()
                    .user(user)
                    .book(book)
                    .liked(liked)
                    .build());
        }

        String message = liked
                ? "Teşekkürler! Önerilerimizi geliştirmemize yardımcı oluyorsun."
                : "Teşekkürler! Bu tür önerileri azaltacağız.";

        return ResponseEntity.ok(Map.of("message", message));
    }

    private String turkishLabel(String label) {
        return switch (label) {
            case "felsefi" -> "Felsefi";
            case "psikolojik_derinlik" -> "Psikolojik derinlik";
            case "duygusal_yogunluk" -> "Duygusal yoğunluk";
            case "toplumsal_elestiri" -> "Toplumsal eleştiri";
            case "tarihsel" -> "Tarihsel";
            case "karamsar" -> "Karamsar";
            case "akici_ve_surukleyici" -> "Akıcı ve sürükleyici";
            case "ask" -> "Aşk";
            case "macera" -> "Macera";
            case "bilimkurgu_distopya" -> "Bilim kurgu/Distopya";
            case "gizem_polisiye" -> "Gizem/Polisiye";
            case "ogretici_farkindalik" -> "Öğretici";
            case "mizah" -> "Mizah";
            default -> label;
        };
    }
}