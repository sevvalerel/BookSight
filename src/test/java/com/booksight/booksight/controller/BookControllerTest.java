package com.booksight.booksight.controller;

import com.booksight.booksight.entity.Book;
import com.booksight.booksight.service.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    @Mock
    private BookService bookService;

    @InjectMocks
    private BookController bookController;

    private Book makeBook(Long id, String title, String genre) {
        Book book = new Book();
        book.setBookId(id);
        book.setTitle(title);
        book.setAuthor("Yazar");
        book.setGenre(genre);
        return book;
    }

    private Map<String, Object> makePageResponse(List<Book> books) {
        return Map.of(
                "content", books,
                "hasNext", false,
                "totalElements", (long) books.size(),
                "totalPages", 1,
                "currentPage", 0
        );
    }

    @Test
    void getAllBooks_parametreSiz_tumKitaplarDonmeli() {
        when(bookService.getAllBooks(null, null, null, 0, 20))
                .thenReturn(makePageResponse(List.of(makeBook(1L, "Suç ve Ceza", "Klasik"))));

        ResponseEntity<Map<String, Object>> response = bookController.getAllBooks(null, null, null, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        List<?> content = (List<?>) response.getBody().get("content");
        assertEquals(1, content.size());
    }

    @Test
    void getAllBooks_searchIle_filtrelenmisKitaplarDonmeli() {
        when(bookService.getAllBooks("suc", null, null, 0, 20))
                .thenReturn(makePageResponse(List.of(makeBook(1L, "Suç ve Ceza", "Klasik"))));

        ResponseEntity<Map<String, Object>> response = bookController.getAllBooks("suc", null, null, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        List<?> content = (List<?>) response.getBody().get("content");
        assertEquals(1, content.size());
    }

    @Test
    void getAllBooks_genreIle_filtrelenmisKitaplarDonmeli() {
        when(bookService.getAllBooks(null, "Roman", null, 0, 20))
                .thenReturn(makePageResponse(List.of(makeBook(1L, "Roman Kitap", "Roman"))));

        ResponseEntity<Map<String, Object>> response = bookController.getAllBooks(null, "Roman", null, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        List<Book> content = (List<Book>) response.getBody().get("content");
        assertEquals("Roman", content.get(0).getGenre());
    }

    @Test
    void getAllBooks_searchVeGenre_filtrelenmisKitaplarDonmeli() {
        when(bookService.getAllBooks("Suç", "Klasik", null, 0, 20))
                .thenReturn(makePageResponse(List.of(makeBook(1L, "Suç ve Ceza", "Klasik"))));

        ResponseEntity<Map<String, Object>> response = bookController.getAllBooks("Suç", "Klasik", null, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        List<?> content = (List<?>) response.getBody().get("content");
        assertEquals(1, content.size());
    }

    @Test
    void getBookById_varolanId_kitapDonmeli() {
        when(bookService.getBookById(1L)).thenReturn(makeBook(1L, "Suç ve Ceza", "Klasik"));

        ResponseEntity<Book> response = bookController.getBookById(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Suç ve Ceza", response.getBody().getTitle());
    }

    @Test
    void getBookById_olmayanId_exceptionFirlatmali() {
        when(bookService.getBookById(99L))
                .thenThrow(new RuntimeException("Kitap bulunamadı!"));

        assertThrows(RuntimeException.class, () -> bookController.getBookById(99L));
    }

    @Test
    void createBook_gecerliKitap_kaydedilmeli() {
        Book book = makeBook(null, "1984", "Distopya");
        when(bookService.createBook(book)).thenReturn(makeBook(1L, "1984", "Distopya"));

        ResponseEntity<Book> response = bookController.createBook(book);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("1984", response.getBody().getTitle());
        verify(bookService).createBook(book);
    }

    @Test
    void updateBook_varolanKitap_guncellenmeli() {
        Book updated = makeBook(1L, "Yeni Başlık", "Roman");
        when(bookService.updateBook(1L, updated)).thenReturn(updated);

        ResponseEntity<Book> response = bookController.updateBook(1L, updated);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Yeni Başlık", response.getBody().getTitle());
    }

    @Test
    void updateBook_olmayanKitap_exceptionFirlatmali() {
        Book updated = makeBook(null, "Başlık", "Roman");
        when(bookService.updateBook(99L, updated))
                .thenThrow(new RuntimeException("Kitap bulunamadı!"));

        assertThrows(RuntimeException.class, () -> bookController.updateBook(99L, updated));
    }

    @Test
    void deleteBook_varolanKitap_silinmeli() {
        doNothing().when(bookService).deleteBook(1L);

        ResponseEntity<?> response = bookController.deleteBook(1L);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Kitap silindi.", body.get("message"));
        verify(bookService).deleteBook(1L);
    }

    @Test
    void deleteBook_olmayanKitap_exceptionFirlatmali() {
        doThrow(new RuntimeException("Kitap bulunamadı!"))
                .when(bookService).deleteBook(99L);

        assertThrows(RuntimeException.class, () -> bookController.deleteBook(99L));
    }
}