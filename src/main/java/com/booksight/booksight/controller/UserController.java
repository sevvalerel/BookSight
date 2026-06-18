package com.booksight.booksight.controller;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.dto.*;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.UserRepository;
import com.booksight.booksight.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(JwtUtil jwtUtil, UserRepository userRepository, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(toDto(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UpdateProfileResponse> updateCurrentUser(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateProfileRequest request) {
        Long userId = getUserIdFromToken(authHeader);
        User user = userService.updateProfile(userId, request);
        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.ok(new UpdateProfileResponse(toDto(user), token));
    }

    @PostMapping("/checkin")
    public ResponseEntity<?> checkin(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        try {
            return ResponseEntity.ok(userService.checkin(userId));
        } catch (RuntimeException e) {
            if ("Bugün zaten okuma kaydettiniz".equals(e.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
            }
            throw e;
        }
    }

    @GetMapping("/checkin/status")
    public ResponseEntity<CheckinResponseDTO> getCheckinStatus(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(userService.getCheckinStatus(userId));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDTO>> getLeaderboard() {
        return ResponseEntity.ok(userService.getLeaderboard());
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserPublicProfileDTO> getUserPublicProfile(
            @PathVariable String username) {
        return ResponseEntity.ok(userService.getUserPublicProfile(username));
    }

    private UserDTO toDto(User user) {
        return new UserDTO(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt().toString(),
                user.getBio(),
                user.getAvatarUrl()
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
