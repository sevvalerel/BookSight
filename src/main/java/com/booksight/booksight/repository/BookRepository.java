package com.booksight.booksight.repository;

import com.booksight.booksight.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByTitleContainingIgnoreCase(String title);
    List<Book> findByGenreContainingIgnoreCase(String genre);
    Page<Book> findByGenreContainingIgnoreCase(String genre, Pageable pageable);
    boolean existsByTitleAndAuthor(String title, String author);
    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title, String author);
    List<Book> findByTitleContainingIgnoreCaseAndGenreContainingIgnoreCase(String title, String genre);
    @Query(value = "SELECT * FROM books WHERE unaccent(LOWER(title)) LIKE unaccent(LOWER(CONCAT('%', :search, '%'))) OR unaccent(LOWER(author)) LIKE unaccent(LOWER(CONCAT('%', :search, '%')))", nativeQuery = true)
    List<Book> searchBooks(@Param("search") String search);

    @Query(value = "SELECT * FROM books WHERE unaccent(LOWER(title)) LIKE unaccent(LOWER(CONCAT('%', :search, '%'))) OR unaccent(LOWER(author)) LIKE unaccent(LOWER(CONCAT('%', :search, '%')))",
            countQuery = "SELECT count(*) FROM books WHERE unaccent(LOWER(title)) LIKE unaccent(LOWER(CONCAT('%', :search, '%'))) OR unaccent(LOWER(author)) LIKE unaccent(LOWER(CONCAT('%', :search, '%')))",
            nativeQuery = true)
    Page<Book> searchBooks(@Param("search") String search, Pageable pageable);

}
