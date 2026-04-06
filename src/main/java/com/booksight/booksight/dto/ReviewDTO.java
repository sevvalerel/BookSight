package com.booksight.booksight.dto;

public class ReviewDTO {
    private Long reviewId;
    private String reviewText;
    private Integer rating;
    private String username;
    private String createdAt;

    public ReviewDTO(Long reviewId, String reviewText, Integer rating, String username, String createdAt) {
        this.reviewId = reviewId;
        this.reviewText = reviewText;
        this.rating = rating;
        this.username = username;
        this.createdAt = createdAt;
    }

    public Long getReviewId() { return reviewId; }
    public String getReviewText() { return reviewText; }
    public Integer getRating() { return rating; }
    public String getUsername() { return username; }
    public String getCreatedAt() { return createdAt; }
}