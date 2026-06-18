package com.booksight.booksight.dto;

import java.util.List;

public class UserPublicProfileDTO {
    private Long userId;
    private String username;
    private String avatarUrl;
    private int totalPoints;
    private int streak;
    private long reviewCount;
    private long readCount;
    private List<String> favoriteGenres;
    private List<SimpleReview> reviews;

    public UserPublicProfileDTO(Long userId, String username, String avatarUrl,
                                 int totalPoints, int streak, long reviewCount,
                                 long readCount, List<String> favoriteGenres,
                                 List<SimpleReview> reviews) {
        this.userId = userId;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.totalPoints = totalPoints;
        this.streak = streak;
        this.reviewCount = reviewCount;
        this.readCount = readCount;
        this.favoriteGenres = favoriteGenres;
        this.reviews = reviews;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getAvatarUrl() { return avatarUrl; }
    public int getTotalPoints() { return totalPoints; }
    public int getStreak() { return streak; }
    public long getReviewCount() { return reviewCount; }
    public long getReadCount() { return readCount; }
    public List<String> getFavoriteGenres() { return favoriteGenres; }
    public List<SimpleReview> getReviews() { return reviews; }

    public static class SimpleReview {
        private String bookTitle;
        private String bookCoverUrl;
        private Integer rating;
        private String reviewText;

        public SimpleReview(String bookTitle, String bookCoverUrl,
                            Integer rating, String reviewText) {
            this.bookTitle = bookTitle;
            this.bookCoverUrl = bookCoverUrl;
            this.rating = rating;
            this.reviewText = reviewText;
        }

        public String getBookTitle() { return bookTitle; }
        public String getBookCoverUrl() { return bookCoverUrl; }
        public Integer getRating() { return rating; }
        public String getReviewText() { return reviewText; }
    }
}
