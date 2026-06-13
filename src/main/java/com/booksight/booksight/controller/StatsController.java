package com.booksight.booksight.controller;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.dto.StatsDTO;
import com.booksight.booksight.repository.UserRepository;
import com.booksight.booksight.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public StatsController(StatsService statsService, JwtUtil jwtUtil, UserRepository userRepository) {
        this.statsService = statsService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<StatsDTO> getMyStats(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(statsService.getStatsForUser(userId));
    }

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"))
                .getUserId();
    }
}