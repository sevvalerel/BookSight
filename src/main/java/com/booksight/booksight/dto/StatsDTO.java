package com.booksight.booksight.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatsDTO {
    private long totalReviews;
    private long reviewsThisMonth;
    private long reviewsThisYear;
    private double averageRating;
    private List<GenreCount> genreDistribution;
    private List<MonthlyCount> monthlyTrend;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GenreCount {
        private String genre;
        private long count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyCount {
        private String month;
        private long count;
    }
}