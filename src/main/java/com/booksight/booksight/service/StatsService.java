package com.booksight.booksight.service;

import com.booksight.booksight.dto.StatsDTO;
import com.booksight.booksight.entity.Review;
import com.booksight.booksight.repository.NlpResultRepository;
import com.booksight.booksight.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final ReviewRepository reviewRepository;
    private final NlpResultRepository nlpResultRepository;

    public StatsService(ReviewRepository reviewRepository, NlpResultRepository nlpResultRepository) {
        this.reviewRepository = reviewRepository;
        this.nlpResultRepository = nlpResultRepository;
    }

    public StatsDTO getStatsForUser(Long userId) {
        List<Review> reviews = reviewRepository.findByUserIdWithBook(userId);

        YearMonth currentMonth = YearMonth.now();
        int currentYear = currentMonth.getYear();

        long totalReviews = reviews.size();

        long reviewsThisMonth = reviews.stream()
                .filter(r -> YearMonth.from(r.getCreatedAt()).equals(currentMonth))
                .count();

        long reviewsThisYear = reviews.stream()
                .filter(r -> r.getCreatedAt().getYear() == currentYear)
                .count();

        double averageRating = reviews.isEmpty() ? 0.0 :
                Math.round(reviews.stream().mapToInt(Review::getRating).average().orElse(0.0) * 10.0) / 10.0;

        Map<String, Long> genreCounts = reviews.stream()
                .map(r -> r.getBook().getGenre())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()));

        List<StatsDTO.GenreCount> genreDistribution = genreCounts.entrySet().stream()
                .map(e -> StatsDTO.GenreCount.builder().genre(e.getKey()).count(e.getValue()).build())
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());

        Map<YearMonth, Long> monthCounts = reviews.stream()
                .collect(Collectors.groupingBy(r -> YearMonth.from(r.getCreatedAt()), Collectors.counting()));

        List<StatsDTO.MonthlyCount> monthlyTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = currentMonth.minusMonths(i);
            monthlyTrend.add(StatsDTO.MonthlyCount.builder()
                    .month(ym.toString())
                    .count(monthCounts.getOrDefault(ym, 0L))
                    .build());
        }

        // ── Sevdiğin Temalar: kullanıcının review'larındaki NLP etiketlerinin frekansı ──
        Map<String, Long> themeCounts = new HashMap<>();
        for (Review r : reviews) {
            nlpResultRepository.findByReview(r).ifPresent(nlp -> {
                if (nlp.getDetectedLabels() != null) {
                    for (String label : nlp.getDetectedLabels()) {
                        themeCounts.merge(label, 1L, Long::sum);
                    }
                }
            });
        }

        List<String> topThemes = themeCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(4)
                .map(e -> turkishLabel(e.getKey()))
                .collect(Collectors.toList());

        return StatsDTO.builder()
                .totalReviews(totalReviews)
                .reviewsThisMonth(reviewsThisMonth)
                .reviewsThisYear(reviewsThisYear)
                .averageRating(averageRating)
                .genreDistribution(genreDistribution)
                .monthlyTrend(monthlyTrend)
                .topThemes(topThemes)
                .build();
    }

    private String turkishLabel(String label) {
        return switch (label) {
            case "felsefi" -> "Felsefi";
            case "psikolojik_derinlik" -> "Psikolojik Derinlik";
            case "duygusal_yogunluk" -> "Duygusal Yoğunluk";
            case "toplumsal_elestiri" -> "Toplumsal Eleştiri";
            case "tarihsel" -> "Tarihsel";
            case "karamsar" -> "Karamsar";
            case "akici_ve_surukleyici" -> "Akıcı ve Sürükleyici";
            case "ask" -> "Aşk";
            case "macera" -> "Macera";
            case "bilimkurgu_distopya" -> "Bilim Kurgu/Distopya";
            case "gizem_polisiye" -> "Gizem/Polisiye";
            case "ogretici_farkindalik" -> "Öğretici/Farkındalık";
            case "mizah" -> "Mizah";
            default -> label;
        };
    }
}