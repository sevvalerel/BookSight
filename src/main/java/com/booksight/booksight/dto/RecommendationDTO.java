package com.booksight.booksight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDTO {
    private Long bookId;
    private String title;
    private String author;
    private String coverUrl;
    private String genre;
    private List<String> detectedLabels;
    private Double confidenceScore;
    private String reason;
}