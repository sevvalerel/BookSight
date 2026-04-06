package com.booksight.booksight.dto;

public class UserDTO {

    private Long userId;
    private String username;
    private String email;
    private String createdAt;

    public UserDTO(Long userId, String username, String email, String createdAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.createdAt = createdAt;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getCreatedAt() { return createdAt; }
}
