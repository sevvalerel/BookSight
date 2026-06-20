package com.booksight.booksight.repository;

import com.booksight.booksight.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBookBookIdOrderByCreatedAtDesc(Long bookId);
    List<Review> findByBookBookIdOrderByRatingDescCreatedAtDesc(Long bookId);
    List<Review> findByBookBookIdOrderByRatingAscCreatedAtDesc(Long bookId);
    List<Review> findByUserUserId(Long userId);
    List<Review> findByUserUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserUserIdAndBookBookId(Long userId, Long bookId);

    @Query("SELECT r FROM Review r JOIN FETCH r.book WHERE r.user.userId = :userId")
    List<Review> findByUserIdWithBook(@Param("userId") Long userId);
}