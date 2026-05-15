package com.booksight.booksight.service;

import com.booksight.booksight.entity.Book;
import com.booksight.booksight.repository.BookRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GoogleBooksService {

    private final RestTemplate restTemplate;
    private final BookRepository bookRepository;

    @Value("${google.books.api.key}")
    private String apiKey;

    @Value("${google.books.api.url}")
    private String apiUrl;

    public GoogleBooksService(BookRepository bookRepository) {
        this.restTemplate = new RestTemplate();
        this.bookRepository = bookRepository;
    }

    public List<Book> fetchAndSaveTurkishBooks(String query, int maxResults) {
        String url = apiUrl + "/volumes?q=intitle:" + query.replace(" ", "+")
                + "&langRestrict=tr"
                + "&maxResults=" + maxResults
                + "&key=" + apiKey;

        Map response = restTemplate.getForObject(url, Map.class);
        List<Book> savedBooks = new ArrayList<>();

        if (response == null) return savedBooks;

        List<Map> items = (List<Map>) response.get("items");
        if (items == null) return savedBooks;

        for (Map item : items) {
            try {
                Map volumeInfo = (Map) item.get("volumeInfo");
                if (volumeInfo == null) continue;

                String title = (String) volumeInfo.get("title");
                List<String> authors = (List<String>) volumeInfo.get("authors");
                String author = (authors != null && !authors.isEmpty())
                        ? authors.get(0) : "Bilinmiyor";

                if (title == null) continue;

                if (bookRepository.existsByTitleAndAuthor(title, author)) continue;

                List<String> categories = (List<String>) volumeInfo.get("categories");
                String genre = (categories != null && !categories.isEmpty())
                        ? categories.get(0) : "Roman";

                String description = (String) volumeInfo.get("description");

                Integer year = null;
                String publishedDate = (String) volumeInfo.get("publishedDate");
                if (publishedDate != null && publishedDate.length() >= 4) {
                    try {
                        year = Integer.parseInt(publishedDate.substring(0, 4));
                    } catch (Exception ignored) {}
                }

                Map imageLinks = (Map) volumeInfo.get("imageLinks");
                String coverUrl = null;
                if (imageLinks != null) {
                    coverUrl = (String) imageLinks.get("thumbnail");
                    if (coverUrl == null) {
                        coverUrl = (String) imageLinks.get("smallThumbnail");
                    }
                    if (coverUrl != null) {
                        coverUrl = coverUrl.replace("http://", "https://");
                        coverUrl = coverUrl.replace("&zoom=1", "&zoom=0");
                        coverUrl = coverUrl.replace("zoom=1", "zoom=0");
                    }
                }

                Book book = new Book();
                book.setTitle(title);
                book.setAuthor(author);
                book.setGenre(genre);
                book.setDescription(description);
                book.setPublicationYear(year);
                book.setCoverUrl(coverUrl);

                savedBooks.add(bookRepository.save(book));

            } catch (Exception e) { }
        }

        return savedBooks;
    }
}