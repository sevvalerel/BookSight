package com.booksight.booksight;

import com.booksight.booksight.entity.Book;
import com.booksight.booksight.repository.BookRepository;
import com.booksight.booksight.service.BookService;
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


}