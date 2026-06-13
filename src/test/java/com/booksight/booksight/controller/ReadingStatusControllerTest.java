package com.booksight.booksight.controller;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.entity.Book;
import com.booksight.booksight.entity.ReadingStatus;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.UserRepository;
import com.booksight.booksight.service.ReadingStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReadingStatusController Tests")
class ReadingStatusControllerTest {

    @Mock private ReadingStatusService readingStatusService;
    @Mock private JwtUtil jwtUtil;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ReadingStatusController readingStatusController;

    private User user;
    private Book book;
    private ReadingStatus readingStatus;
    private final String AUTH_HEADER = "Bearer test-token";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setUsername("testuser");

        book = new Book();
        book.setBookId(10L);
        book.setTitle("Test Kitap");
        book.setAuthor("Test Yazar");
        book.setCoverUrl("http://cover.jpg");

        readingStatus = ReadingStatus.builder()
                .user(user)
                .book(book)
                .status("READING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(jwtUtil.extractUsername("test-token")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    }

    @Nested
    @DisplayName("POST /api/reading-status")
    class AddOrUpdate {

        @Test
        @DisplayName("Geçerli istek ile 200 ve DTO döner")
        void shouldReturn200WithDto() {
            Map<String, Object> request = Map.of("bookId", "10", "status", "READING");
            when(readingStatusService.addOrUpdate(1L, 10L, "READING")).thenReturn(readingStatus);

            ResponseEntity<?> response = readingStatusController.addOrUpdate(AUTH_HEADER, request);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("Service çağrısı doğru parametrelerle yapılır")
        void shouldCallServiceWithCorrectParams() {
            Map<String, Object> request = Map.of("bookId", "10", "status", "READ");
            when(readingStatusService.addOrUpdate(1L, 10L, "READ")).thenReturn(readingStatus);

            readingStatusController.addOrUpdate(AUTH_HEADER, request);

            verify(readingStatusService).addOrUpdate(1L, 10L, "READ");
        }
    }

    @Nested
    @DisplayName("GET /api/reading-status/my")
    class GetMyLibrary {

        @Test
        @DisplayName("Kütüphane listesi 200 ile döner")
        void shouldReturn200WithLibrary() {
            when(readingStatusService.getMyLibrary(1L)).thenReturn(List.of(readingStatus));

            ResponseEntity<?> response = readingStatusController.getMyLibrary(AUTH_HEADER);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat((List<?>) response.getBody()).hasSize(1);
        }

        @Test
        @DisplayName("Boş kütüphane için boş liste döner")
        void shouldReturnEmptyListWhenLibraryIsEmpty() {
            when(readingStatusService.getMyLibrary(1L)).thenReturn(List.of());

            ResponseEntity<?> response = readingStatusController.getMyLibrary(AUTH_HEADER);

            assertThat((List<?>) response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("DELETE /api/reading-status/{bookId}")
    class RemoveFromLibrary {

        @Test
        @DisplayName("Silme işlemi başarılı, 200 ve mesaj döner")
        void shouldReturn200WithMessage() {
            doNothing().when(readingStatusService).removeFromLibrary(1L, 10L);

            ResponseEntity<?> response = readingStatusController.removeFromLibrary(10L, AUTH_HEADER);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(((Map<?, ?>) response.getBody()).get("message"))
                    .isEqualTo("Kitap kütüphaneden çıkarıldı.");
        }

        @Test
        @DisplayName("Service removeFromLibrary doğru parametrelerle çağrılır")
        void shouldCallServiceWithCorrectParams() {
            doNothing().when(readingStatusService).removeFromLibrary(1L, 10L);

            readingStatusController.removeFromLibrary(10L, AUTH_HEADER);

            verify(readingStatusService).removeFromLibrary(1L, 10L);
        }
    }
}