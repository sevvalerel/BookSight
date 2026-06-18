package com.booksight.booksight.dto;

public class ReviewDTO {
    private Long reviewId;
    private String reviewText;
    private Integer rating;
    private String username;
    private String createdAt;
    private Long bookId;
    private String bookTitle;
    private String bookCoverUrl;

    public ReviewDTO(Long reviewId, String reviewText, Integer rating, String username, String createdAt,
                     Long bookId, String bookTitle, String bookCoverUrl) {
        this.reviewId = reviewId;
        this.reviewText = reviewText;
        this.rating = rating;
        this.username = username;
        this.createdAt = createdAt;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.bookCoverUrl = bookCoverUrl;
    }

    public Long getReviewId() { return reviewId; }
    public String getReviewText() { return reviewText; }
    public Integer getRating() { return rating; }
    public String getUsername() { return username; }
    public String getCreatedAt() { return createdAt; }
    public Long getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public String getBookCoverUrl() { return bookCoverUrl; }
}