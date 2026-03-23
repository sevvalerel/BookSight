package com.booksight.booksight.service;

import com.booksight.booksight.entity.Book;
import com.booksight.booksight.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GoogleBooksService {

    private final RestTemplate restTemplate;
    private final BookRepository bookRepository;

    public GoogleBooksService(BookRepository bookRepository) {
        this.restTemplate = new RestTemplate();
        this.bookRepository = bookRepository;
    }

    public List<Book> fetchAndSaveTurkishBooks(String query, int maxResults) {
        String url = "http://digittall.duckdns.org:5010/kitapi/v1/?yazar=" +
                query.replace(" ", "%20");

        Map response = restTemplate.getForObject(url, Map.class);

        List<Book> savedBooks = new ArrayList<>();

        if (response == null) return savedBooks;

        Integer code = (Integer) response.get("code");
        if (code == null || code != 200) return savedBooks;

        List<Map> items = (List<Map>) response.get("message");
        if (items == null) return savedBooks;

        int count = 0;
        for (Map item : items) {
            if (count >= maxResults) break;
            try {
                String title = (String) item.get("kitap_adi");
                String author = (String) item.get("yazar");

                if (title == null || author == null) continue;
                if (bookRepository.existsByTitleAndAuthor(title, author)) continue;

                List<String> kategoriler = (List<String>) item.get("kategori");
                String genre = "Roman";
                if (kategoriler != null) {
                    genre = kategoriler.stream()
                            .filter(k -> !k.equals("Kitap"))
                            .findFirst()
                            .orElse("Roman");
                }

                Book book = new Book();
                book.setTitle(title);
                book.setAuthor(author);
                book.setGenre(genre);

                savedBooks.add(bookRepository.save(book));
                count++;

            } catch (Exception e) { }
        }

        return savedBooks;
    }
}