package com.booksight.booksight.service;

import com.booksight.booksight.dto.StatsDTO;
import com.booksight.booksight.entity.Review;
import com.booksight.booksight.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final ReviewRepository reviewRepository;

    public StatsService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
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

        return StatsDTO.builder()
                .totalReviews(totalReviews)
                .reviewsThisMonth(reviewsThisMonth)
                .reviewsThisYear(reviewsThisYear)
                .averageRating(averageRating)
                .genreDistribution(genreDistribution)
                .monthlyTrend(monthlyTrend)
                .build();
    }
}