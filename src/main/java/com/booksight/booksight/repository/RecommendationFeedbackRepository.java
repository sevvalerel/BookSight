package com.booksight.booksight.repository;

import com.booksight.booksight.entity.RecommendationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RecommendationFeedbackRepository extends JpaRepository<RecommendationFeedback, Long> {
    Optional<RecommendationFeedback> findByUserUserIdAndBookBookId(Long userId, Long bookId);
    List<RecommendationFeedback> findByUserUserIdAndLikedFalse(Long userId);
    List<RecommendationFeedback> findByUserUserId(Long userId);
}
