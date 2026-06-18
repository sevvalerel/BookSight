package com.booksight.booksight.dto;

public class LeaderboardEntryDTO {
    private Long userId;
    private String username;
    private String avatarUrl;
    private int totalPoints;
    private int streak;

    public LeaderboardEntryDTO(Long userId, String username, String avatarUrl,
                                int totalPoints, int streak) {
        this.userId = userId;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.totalPoints = totalPoints;
        this.streak = streak;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getAvatarUrl() { return avatarUrl; }
    public int getTotalPoints() { return totalPoints; }
    public int getStreak() { return streak; }
}
