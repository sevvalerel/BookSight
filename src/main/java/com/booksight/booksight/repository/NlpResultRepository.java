package com.booksight.booksight.repository;

import com.booksight.booksight.entity.NlpResult;
import com.booksight.booksight.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NlpResultRepository extends JpaRepository<NlpResult, Long> {
    Optional<NlpResult> findByReview(Review review);
}