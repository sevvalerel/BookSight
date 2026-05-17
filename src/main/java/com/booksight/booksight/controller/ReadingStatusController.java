package com.booksight.booksight.controller;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.dto.ReadingStatusDTO;
import com.booksight.booksight.entity.ReadingStatus;
import com.booksight.booksight.repository.UserRepository;
import com.booksight.booksight.service.ReadingStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reading-status")
public class ReadingStatusController {

    private final ReadingStatusService readingStatusService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public ReadingStatusController(ReadingStatusService readingStatusService,
                                   JwtUtil jwtUtil,
                                   UserRepository userRepository) {
        this.readingStatusService = readingStatusService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ReadingStatusDTO> addOrUpdate(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        Long userId = getUserIdFromToken(authHeader);
        Long bookId = Long.valueOf(request.get("bookId").toString());
        String status = request.get("status").toString();

        ReadingStatus saved = readingStatusService.addOrUpdate(userId, bookId, status);
        return ResponseEntity.ok(toDto(saved));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReadingStatusDTO>> getMyLibrary(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        List<ReadingStatusDTO> dtos = readingStatusService.getMyLibrary(userId).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Map<String, String>> removeFromLibrary(
            @PathVariable Long bookId,
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        readingStatusService.removeFromLibrary(userId, bookId);
        return ResponseEntity.ok(Map.of("message", "Kitap kütüphaneden çıkarıldı."));
    }

    private ReadingStatusDTO toDto(ReadingStatus rs) {
        String updatedAt = rs.getUpdatedAt() != null
                ? rs.getUpdatedAt().toString()
                : rs.getCreatedAt().toString();
        return new ReadingStatusDTO(
                rs.getBook().getBookId(),
                rs.getBook().getTitle(),
                rs.getBook().getAuthor(),
                rs.getBook().getCoverUrl(),
                rs.getStatus(),
                updatedAt
        );
    }

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"))
                .getUserId();
    }
}
