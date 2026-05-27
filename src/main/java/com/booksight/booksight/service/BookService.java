package com.booksight.booksight.service;

import com.booksight.booksight.entity.Book;
import com.booksight.booksight.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Map<String, Object> getAllBooks(String search, String genre, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> bookPage;

        if (search != null && !search.isBlank()) {
            bookPage = bookRepository.searchBooks(search, pageable);
        } else if (genre != null && !genre.isBlank()) {
            bookPage = bookRepository.findByGenreContainingIgnoreCase(genre, pageable);
        } else {
            bookPage = bookRepository.findAll(pageable);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", bookPage.getContent());
        response.put("totalElements", bookPage.getTotalElements());
        response.put("totalPages", bookPage.getTotalPages());
        response.put("currentPage", page);
        response.put("hasNext", bookPage.hasNext());
        return response;
    }

    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kitap bulunamadı!"));
    }

    public Book createBook(Book book) {
        return bookRepository.save(book);
    }

    public Book updateBook(Long id, Book updatedBook) {
        Book existing = getBookById(id);
        existing.setTitle(updatedBook.getTitle());
        existing.setAuthor(updatedBook.getAuthor());
        existing.setGenre(updatedBook.getGenre());
        existing.setPublicationYear(updatedBook.getPublicationYear());
        existing.setDescription(updatedBook.getDescription());
        existing.setCoverUrl(updatedBook.getCoverUrl());
        return bookRepository.save(existing);
    }

    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new RuntimeException("Kitap bulunamadı!");
        }
        bookRepository.deleteById(id);
    }


}
