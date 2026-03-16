package com.booksight.booksight.service;

import com.booksight.booksight.entity.Book;
import com.booksight.booksight.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks(String search, String genre) {
        if (search != null && !search.isBlank() && genre != null && !genre.isBlank()) {
            return bookRepository.findByTitleContainingIgnoreCaseAndGenreContainingIgnoreCase(search, genre);
        } else if (search != null && !search.isBlank()) {
            return bookRepository.findByTitleContainingIgnoreCase(search);
        } else if (genre != null && !genre.isBlank()) {
            return bookRepository.findByGenreContainingIgnoreCase(genre);
        }
        return bookRepository.findAll();
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
        return bookRepository.save(existing);
    }

    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new RuntimeException("Kitap bulunamadı!");
        }
        bookRepository.deleteById(id);
    }
}
