package com.booksight.booksight.service;

import com.booksight.booksight.entity.Book;
import com.booksight.booksight.entity.ReadingStatus;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.BookRepository;
import com.booksight.booksight.repository.ReadingStatusRepository;
import com.booksight.booksight.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReadingStatusService Tests")
class ReadingStatusServiceTest {

    @Mock private ReadingStatusRepository readingStatusRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookRepository bookRepository;

    @InjectMocks
    private ReadingStatusService readingStatusService;

    private User user;
    private Book book;
    private ReadingStatus readingStatus;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);

        book = new Book();
        book.setBookId(10L);

        readingStatus = ReadingStatus.builder()
                .user(user)
                .book(book)
                .status("READING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("addOrUpdate()")
    class AddOrUpdate {

        @Test
        @DisplayName("Geçersiz status için RuntimeException fırlatır")
        void shouldThrowForInvalidStatus() {
            assertThatThrownBy(() -> readingStatusService.addOrUpdate(1L, 10L, "INVALID"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Geçersiz okuma durumu");
        }

        @Test
        @DisplayName("Olmayan userId için RuntimeException fırlatır")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> readingStatusService.addOrUpdate(1L, 10L, "READING"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Kullanıcı bulunamadı");
        }

        @Test
        @DisplayName("Olmayan bookId için RuntimeException fırlatır")
        void shouldThrowWhenBookNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(bookRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> readingStatusService.addOrUpdate(1L, 10L, "READING"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Kitap bulunamadı");
        }

        @Test
        @DisplayName("Yeni kayıt oluşturur ve kaydeder")
        void shouldCreateNewReadingStatus() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
            when(readingStatusRepository.findByUserUserIdAndBookBookId(1L, 10L)).thenReturn(Optional.empty());
            when(readingStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReadingStatus result = readingStatusService.addOrUpdate(1L, 10L, "READING");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("READING");
            assertThat(result.getUser()).isEqualTo(user);
            assertThat(result.getBook()).isEqualTo(book);
            verify(readingStatusRepository, times(1)).save(any(ReadingStatus.class));
        }

        @Test
        @DisplayName("Mevcut kaydı günceller")
        void shouldUpdateExistingReadingStatus() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
            when(readingStatusRepository.findByUserUserIdAndBookBookId(1L, 10L)).thenReturn(Optional.of(readingStatus));
            when(readingStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReadingStatus result = readingStatusService.addOrUpdate(1L, 10L, "READ");

            assertThat(result.getStatus()).isEqualTo("READ");
            verify(readingStatusRepository, times(1)).save(readingStatus);
        }

        @Test
        @DisplayName("Tüm geçerli statusler kabul edilir")
        void shouldAcceptAllValidStatuses() {
            for (String status : List.of("READING", "WILL_READ", "READ")) {
                when(userRepository.findById(1L)).thenReturn(Optional.of(user));
                when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
                when(readingStatusRepository.findByUserUserIdAndBookBookId(1L, 10L)).thenReturn(Optional.empty());
                when(readingStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                ReadingStatus result = readingStatusService.addOrUpdate(1L, 10L, status);

                assertThat(result.getStatus()).isEqualTo(status);
            }
        }
    }

    @Nested
    @DisplayName("getMyLibrary()")
    class GetMyLibrary {

        @Test
        @DisplayName("Geçerli userId ile kütüphane listesi döner")
        void shouldReturnLibraryForValidUser() {
            when(userRepository.existsById(1L)).thenReturn(true);
            when(readingStatusRepository.findByUserUserId(1L)).thenReturn(List.of(readingStatus));

            List<ReadingStatus> result = readingStatusService.getMyLibrary(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(readingStatus);
        }

        @Test
        @DisplayName("Olmayan userId için RuntimeException fırlatır")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> readingStatusService.getMyLibrary(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Kullanıcı bulunamadı");
        }

        @Test
        @DisplayName("Kütüphanesi boş kullanıcı için boş liste döner")
        void shouldReturnEmptyListWhenLibraryIsEmpty() {
            when(userRepository.existsById(1L)).thenReturn(true);
            when(readingStatusRepository.findByUserUserId(1L)).thenReturn(List.of());

            List<ReadingStatus> result = readingStatusService.getMyLibrary(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("removeFromLibrary()")
    class RemoveFromLibrary {

        @Test
        @DisplayName("Mevcut kayıt silinir")
        void shouldDeleteExistingStatus() {
            when(readingStatusRepository.findByUserUserIdAndBookBookId(1L, 10L))
                    .thenReturn(Optional.of(readingStatus));

            readingStatusService.removeFromLibrary(1L, 10L);

            verify(readingStatusRepository, times(1)).deleteByUserUserIdAndBookBookId(1L, 10L);
        }

        @Test
        @DisplayName("Kütüphanede olmayan kitap için RuntimeException fırlatır")
        void shouldThrowWhenStatusNotFound() {
            when(readingStatusRepository.findByUserUserIdAndBookBookId(1L, 10L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> readingStatusService.removeFromLibrary(1L, 10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Kitap kütüphanede bulunamadı");
        }
    }
}