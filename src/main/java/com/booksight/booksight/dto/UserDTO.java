package com.booksight.booksight.dto;

public class UserDTO {

    private Long userId;
    private String username;
    private String email;
    private String createdAt;
    private String bio;
    private String avatarUrl;

    public UserDTO(Long userId, String username, String email, String createdAt,
                   String bio, String avatarUrl) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.createdAt = createdAt;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getCreatedAt() { return createdAt; }
    public String getBio() { return bio; }
    public String getAvatarUrl() { return avatarUrl; }
}
