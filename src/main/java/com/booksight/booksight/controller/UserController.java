package com.booksight.booksight.controller;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.dto.UserDTO;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public UserController(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        UserDTO dto = new UserDTO(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt().toString()
        );
        return ResponseEntity.ok(dto);
    }
}
