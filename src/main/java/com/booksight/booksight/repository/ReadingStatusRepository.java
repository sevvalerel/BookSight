package com.booksight.booksight.repository;

import com.booksight.booksight.entity.ReadingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingStatusRepository extends JpaRepository<ReadingStatus, Long> {

    List<ReadingStatus> findByUserUserId(Long userId);

    Optional<ReadingStatus> findByUserUserIdAndBookBookId(Long userId, Long bookId);

    void deleteByUserUserIdAndBookBookId(Long userId, Long bookId);
}
