package com.booksight.booksight.controller;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.dto.RecommendationDTO;
import com.booksight.booksight.entity.NlpResult;
import com.booksight.booksight.repository.NlpResultRepository;
import com.booksight.booksight.repository.ReviewRepository;
import com.booksight.booksight.repository.UserRepository;
import com.booksight.booksight.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final NlpResultRepository nlpResultRepository;
    private final ReviewRepository reviewRepository;

    @GetMapping
    public ResponseEntity<List<RecommendationDTO>> getRecommendations(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"))
                .getUserId();

        var recommendations = recommendationService.getRecommendations(userId);

        List<RecommendationDTO> result = recommendations.stream().map(rec -> {
            var book = rec.getBook();

            // Kitabın NLP etiketlerini bul
            List<String> detectedLabels = new ArrayList<>();
            Double confidenceScore = 0.0;

            var reviews = reviewRepository.findByBookBookIdOrderByCreatedAtDesc(book.getBookId());
            for (var review : reviews) {
                var nlpOpt = nlpResultRepository.findByReview(review);
                if (nlpOpt.isPresent()) {
                    NlpResult nlp = nlpOpt.get();
                    if (nlp.getDetectedLabels() != null) {
                        for (String label : nlp.getDetectedLabels()) {
                            if (!detectedLabels.contains(label)) {
                                detectedLabels.add(label);
                            }
                        }
                    }
                    if (nlp.getLabelScores() != null && !nlp.getLabelScores().isEmpty()) {
                        double avg = nlp.getLabelScores().values().stream()
                                .mapToDouble(Double::doubleValue).average().orElse(0);
                        confidenceScore = Math.max(confidenceScore, avg);
                    }
                }
            }

            // Neden önerildi metni
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
                    .confidenceScore(rec.getConfidenceScore() != null ? rec.getConfidenceScore() : confidenceScore)
                    .reason(reason)
                    .build();
        }).toList();

        return ResponseEntity.ok(result);
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