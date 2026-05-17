package com.booksight.booksight.dto;

public class ReadingStatusDTO {
    private Long bookId;
    private String title;
    private String author;
    private String coverUrl;
    private String status;
    private String updatedAt;

    public ReadingStatusDTO(Long bookId, String title, String author, String coverUrl,
                            String status, String updatedAt) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.coverUrl = coverUrl;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public Long getBookId() { return bookId; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCoverUrl() { return coverUrl; }
    public String getStatus() { return status; }
    public String getUpdatedAt() { return updatedAt; }
}
