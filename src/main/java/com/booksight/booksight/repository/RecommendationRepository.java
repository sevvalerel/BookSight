package com.booksight.booksight.repository;

import com.booksight.booksight.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByUserUserIdOrderByCreatedAtDesc(Long userId);
}
