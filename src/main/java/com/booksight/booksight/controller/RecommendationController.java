package com.booksight.booksight.controller;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.entity.Recommendation;
import com.booksight.booksight.repository.UserRepository;
import com.booksight.booksight.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public RecommendationController(RecommendationService recommendationService,
                                     JwtUtil jwtUtil,
                                     UserRepository userRepository) {
        this.recommendationService = recommendationService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<Recommendation>> getRecommendations(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"))
                .getUserId();
        return ResponseEntity.ok(recommendationService.getRecommendations(userId));
    }
}
