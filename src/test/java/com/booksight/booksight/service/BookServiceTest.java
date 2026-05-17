package com.booksight.booksight.service;

import com.booksight.booksight.entity.Book;
import com.booksight.booksight.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

        // ACT
        Book result = bookService.getBookById(1L);

        // ASSERT
        assertNotNull(result);
        assertEquals("Suç ve Ceza", result.getTitle());
        assertEquals("Dostoyevski", result.getAuthor());

    }
    @Test
    void getBookById_olmayanId_hataDonmeli() {
        // ARRANGE
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(RuntimeException.class, () ->
                bookService.getBookById(99L)
        );
    }
    @Test
    void getAllBooks_herIkiParametreNull_tumKitaplarDonmeli() {
        // ARRANGE
        Book book1 = new Book();
        book1.setTitle("Suç ve Ceza");
        Book book2 = new Book();
        book2.setTitle("1984");

        when(bookRepository.findAll()).thenReturn(List.of(book1, book2));

        // ACT
        List<Book> result = bookService.getAllBooks(null, null);

        // ASSERT
        assertEquals(2, result.size());
    }

    @Test
    void getAllBooks_sadeceSaarchVar_basligaGoreFiltreler() {
        // ARRANGE
        Book book = new Book();
        book.setTitle("Suç ve Ceza");

        when(bookRepository.searchBooks("Suç"))
                .thenReturn(List.of(book));

        // ACT
        List<Book> result = bookService.getAllBooks("Suç", null);

        // ASSERT
        assertEquals(1, result.size());
        assertEquals("Suç ve Ceza", result.get(0).getTitle());
    }
    @Test
    void deleteBook_varolanKitap_silinmeli() {
        // ARRANGE
        when(bookRepository.existsById(1L)).thenReturn(true);

        // ACT
        bookService.deleteBook(1L);

        // ASSERT
        verify(bookRepository).deleteById(1L);
    }

    @Test
    void deleteBook_olmayanKitap_hataDonmeli() {
        // ARRANGE
        when(bookRepository.existsById(99L)).thenReturn(false);

        // ACT & ASSERT
        assertThrows(RuntimeException.class, () ->
                bookService.deleteBook(99L)
        );
    }
    @Test
    void createBook_gecerliKitap_kaydedilmeli() {
        // ARRANGE
        Book book = new Book();
        book.setTitle("1984");
        book.setAuthor("George Orwell");

        when(bookRepository.save(book)).thenReturn(book);

        // ACT
        Book result = bookService.createBook(book);

        // ASSERT
        assertNotNull(result);
        assertEquals("1984", result.getTitle());
        verify(bookRepository).save(book);
    }
    @Test
    void getAllBooks_sadece_genre_var_tureDonmeli() {
        Book book = new Book();
        book.setTitle("Suç ve Ceza");
        book.setGenre("Klasik");

        when(bookRepository.findByGenreContainingIgnoreCase("Klasik"))
                .thenReturn(List.of(book));

        List<Book> result = bookService.getAllBooks(null, "Klasik");

        assertEquals(1, result.size());
        assertEquals("Klasik", result.get(0).getGenre());
    }

    @Test
    void getAllBooks_searchVeGenreVar_ikiliFiltreleme() {
        Book book = new Book();
        book.setTitle("Suç ve Ceza");
        book.setGenre("Klasik");

        when(bookRepository.findByTitleContainingIgnoreCaseAndGenreContainingIgnoreCase("Suç", "Klasik"))
                .thenReturn(List.of(book));

        List<Book> result = bookService.getAllBooks("Suç", "Klasik");

        assertEquals(1, result.size());
        assertEquals("Suç ve Ceza", result.get(0).getTitle());
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

        assertThrows(RuntimeException.class, () ->
                bookService.updateBook(99L, updated)
        );
        verify(bookRepository, never()).save(any());
    }


}