package com.booksight.booksight.controller;

import com.booksight.booksight.entity.User;
import com.booksight.booksight.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");

        User user = userService.register(username, email, password);
        return ResponseEntity.ok(Map.of(
                "message", "Kayıt başarılı!",
                "username", user.getUsername()
        ));
    }
}