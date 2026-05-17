package com.booksight.booksight.service;

import com.booksight.booksight.entity.Book;
import com.booksight.booksight.entity.ReadingStatus;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.BookRepository;
import com.booksight.booksight.repository.ReadingStatusRepository;
import com.booksight.booksight.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReadingStatusService {

    private static final List<String> VALID_STATUSES = List.of("READING", "WILL_READ", "READ");

    private final ReadingStatusRepository readingStatusRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public ReadingStatusService(ReadingStatusRepository readingStatusRepository,
                                UserRepository userRepository,
                                BookRepository bookRepository) {
        this.readingStatusRepository = readingStatusRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    public ReadingStatus addOrUpdate(Long userId, Long bookId, String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new RuntimeException("Geçersiz okuma durumu: " + status);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Kitap bulunamadı!"));

        ReadingStatus existing = readingStatusRepository
                .findByUserUserIdAndBookBookId(userId, bookId)
                .orElse(null);

        LocalDateTime now = LocalDateTime.now();

        if (existing != null) {
            existing.setStatus(status);
            existing.setUpdatedAt(now);
            return readingStatusRepository.save(existing);
        }

        ReadingStatus readingStatus = ReadingStatus.builder()
                .user(user)
                .book(book)
                .status(status)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return readingStatusRepository.save(readingStatus);
    }

    public List<ReadingStatus> getMyLibrary(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Kullanıcı bulunamadı!");
        }
        return readingStatusRepository.findByUserUserId(userId);
    }

    public void removeFromLibrary(Long userId, Long bookId) {
        if (!readingStatusRepository.findByUserUserIdAndBookBookId(userId, bookId).isPresent()) {
            throw new RuntimeException("Kitap kütüphanede bulunamadı!");
        }
        readingStatusRepository.deleteByUserUserIdAndBookBookId(userId, bookId);
    }
}
