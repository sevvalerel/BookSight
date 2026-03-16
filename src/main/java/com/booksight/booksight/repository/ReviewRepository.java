package com.booksight.booksight.repository;

import com.booksight.booksight.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBookBookIdOrderByCreatedAtDesc(Long bookId);
    List<Review> findByUserUserId(Long userId);
    boolean existsByUserUserIdAndBookBookId(Long userId, Long bookId);
}
