package com.booksight.booksight.entity;

import com.booksight.booksight.entity.Review;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;


import java.util.List;
import java.util.Map;

@Data
@Entity
@Table(name = "nlp_results")
public class NlpResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    // ["felsefi", "ask", "tarihsel"]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detected_labels", columnDefinition = "jsonb")
    private List<String> detectedLabels;

    // {"felsefi": 0.82, "ask": 0.31, ...}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "label_scores", columnDefinition = "jsonb")
    private Map<String, Double> labelScores;

    @Column(name = "processed_at")
    private String processedAt;
}