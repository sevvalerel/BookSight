package com.booksight.booksight.service;

import com.booksight.booksight.entity.Book;
import com.booksight.booksight.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void getBookById_varolanId_kitapDonmeli() {
        Book book = new Book();
        book.setBookId(1L);
        book.setTitle("Suç ve Ceza");
        book.setAuthor("Dostoyevski");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        Book result = bookService.getBookById(1L);

        assertNotNull(result);
        assertEquals("Suç ve Ceza", result.getTitle());
        assertEquals("Dostoyevski", result.getAuthor());
    }

    @Test
    void getBookById_olmayanId_hataDonmeli() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> bookService.getBookById(99L));
    }

    @Test
    void getAllBooks_herIkiParametreNull_tumKitaplarDonmeli() {
        Book book1 = new Book();
        book1.setTitle("Suç ve Ceza");
        Book book2 = new Book();
        book2.setTitle("1984");

        Page<Book> page = new PageImpl<>(List.of(book1, book2));
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(page);

        Map<String, Object> result = bookService.getAllBooks(null, null, null, null, 0, 20);

        assertEquals(2, ((List<?>) result.get("content")).size());
        assertEquals(false, result.get("hasNext"));
    }

    @Test
    void getAllBooks_sadeceSaarchVar_basligaGoreFiltreler() {
        Book book = new Book();
        book.setTitle("Suç ve Ceza");

        Page<Book> page = new PageImpl<>(List.of(book));
        when(bookRepository.searchBooks(eq("Suç"), any(Pageable.class))).thenReturn(page);

        Map<String, Object> result = bookService.getAllBooks("Suç", null, null, null, 0, 20);

        assertEquals(1, ((List<?>) result.get("content")).size());
        assertEquals("Suç ve Ceza", ((List<Book>) result.get("content")).get(0).getTitle());
    }

    @Test
    void getAllBooks_sadece_genre_var_tureDonmeli() {
        Book book = new Book();
        book.setTitle("Suç ve Ceza");
        book.setGenre("Klasik");

        Page<Book> page = new PageImpl<>(List.of(book));
        when(bookRepository.findByGenreContainingIgnoreCase(eq("Klasik"), any(Pageable.class)))
                .thenReturn(page);

        Map<String, Object> result = bookService.getAllBooks(null, "Klasik", null, null, 0, 20);

        assertEquals(1, ((List<?>) result.get("content")).size());
        assertEquals("Klasik", ((List<Book>) result.get("content")).get(0).getGenre());
    }

    @Test
    void getAllBooks_searchVeGenreVar_searchOncelikli() {
        Book book = new Book();
        book.setTitle("Suç ve Ceza");
        book.setGenre("Klasik");

        Page<Book> page = new PageImpl<>(List.of(book));
        when(bookRepository.searchBooks(eq("Suç"), any(Pageable.class))).thenReturn(page);

        Map<String, Object> result = bookService.getAllBooks("Suç", "Klasik", null, null, 0, 20);

        assertEquals(1, ((List<?>) result.get("content")).size());
    }

    @Test
    void deleteBook_varolanKitap_silinmeli() {
        when(bookRepository.existsById(1L)).thenReturn(true);

        bookService.deleteBook(1L);

        verify(bookRepository).deleteById(1L);
    }

    @Test
    void deleteBook_olmayanKitap_hataDonmeli() {
        when(bookRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> bookService.deleteBook(99L));
    }

    @Test
    void createBook_gecerliKitap_kaydedilmeli() {
        Book book = new Book();
        book.setTitle("1984");
        book.setAuthor("George Orwell");

        when(bookRepository.save(book)).thenReturn(book);

        Book result = bookService.createBook(book);

        assertNotNull(result);
        assertEquals("1984", result.getTitle());
        verify(bookRepository).save(book);
    }

    @Test
    void updateBook_varolanKitap_guncellenmeli() {
        Book existing = new Book();
        existing.setBookId(1L);
        existing.setTitle("Eski Başlık");
        existing.setAuthor("Eski Yazar");

        Book updated = new Book();
        updated.setTitle("Yeni Başlık");
        updated.setAuthor("Yeni Yazar");
        updated.setGenre("Roman");
        updated.setPublicationYear(2020);
        updated.setDescription("Yeni açıklama");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));

        Book result = bookService.updateBook(1L, updated);

        assertEquals("Yeni Başlık", result.getTitle());
        assertEquals("Yeni Yazar", result.getAuthor());
        verify(bookRepository).save(existing);
    }

    @Test
    void updateBook_olmayanKitap_hataDonmeli() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        Book updated = new Book();
        updated.setTitle("Başlık");

        assertThrows(RuntimeException.class, () -> bookService.updateBook(99L, updated));
        verify(bookRepository, never()).save(any());
    }
}