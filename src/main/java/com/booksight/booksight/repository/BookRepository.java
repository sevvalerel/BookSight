package com.booksight.booksight.repository;

import com.booksight.booksight.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByTitleContainingIgnoreCase(String title);
    List<Book> findByGenreContainingIgnoreCase(String genre);
    List<Book> findByTitleContainingIgnoreCaseAndGenreContainingIgnoreCase(String title, String genre);
}
