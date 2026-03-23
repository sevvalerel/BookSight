package com.booksight.booksight.controller;

import com.booksight.booksight.entity.Book;
import com.booksight.booksight.service.GoogleBooksService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/books")
public class GoogleBooksController {

    private final GoogleBooksService googleBooksService;

    public GoogleBooksController(GoogleBooksService googleBooksService) {
        this.googleBooksService = googleBooksService;
    }

    @PostMapping("/fetch")
    public ResponseEntity<List<Book>> fetchBooks(
            @RequestParam(defaultValue = "roman") String query,
            @RequestParam(defaultValue = "20") int maxResults) {
        return ResponseEntity.ok(googleBooksService.fetchAndSaveTurkishBooks(query, maxResults));
    }
}