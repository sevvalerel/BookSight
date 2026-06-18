package com.booksight.booksight.dto;

public class CheckinResponseDTO {
    private int streak;
    private int totalPoints;
    private boolean alreadyCheckedIn;

    public CheckinResponseDTO(int streak, int totalPoints, boolean alreadyCheckedIn) {
        this.streak = streak;
        this.totalPoints = totalPoints;
        this.alreadyCheckedIn = alreadyCheckedIn;
    }

    public int getStreak() { return streak; }
    public int getTotalPoints() { return totalPoints; }
    public boolean isAlreadyCheckedIn() { return alreadyCheckedIn; }
}
