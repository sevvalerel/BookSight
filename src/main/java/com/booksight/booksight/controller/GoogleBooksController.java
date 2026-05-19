package com.booksight.booksight.controller;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.entity.Book;
import com.booksight.booksight.repository.UserRepository;
import com.booksight.booksight.service.BookService;
import com.booksight.booksight.service.GoogleBooksService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/books")
public class GoogleBooksController {

    private final GoogleBooksService googleBooksService;
    private final BookService bookService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public GoogleBooksController(GoogleBooksService googleBooksService,
                                 BookService bookService,
                                 JwtUtil jwtUtil,
                                 UserRepository userRepository) {
        this.googleBooksService = googleBooksService;
        this.bookService = bookService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @PostMapping("/fetch")
    public ResponseEntity<List<Book>> fetchBooks(
            @RequestParam(defaultValue = "roman") String query,
            @RequestParam(defaultValue = "20") int maxResults) {
        return ResponseEntity.ok(googleBooksService.fetchAndSaveTurkishBooks(query, maxResults));
    }

    @PostMapping("/refresh-covers")
    public ResponseEntity<Map<String, Integer>> refreshCovers(
            @RequestHeader("Authorization") String authHeader) {
        validateToken(authHeader);
        return ResponseEntity.ok(bookService.refreshAllCovers());
    }

    private void validateToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));
    }
}