package com.booksight.booksight.service;

import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.UserRepository;
import org.springframework.stereotype.Service;
import com.booksight.booksight.config.JwtUtil;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String username, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Bu email zaten kayıtlı!");
        }
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Bu kullanıcı adı zaten alınmış!");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(password)
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));
    }
    public String login(String email, String password, JwtUtil jwtUtil) {
        User user = findByEmail(email);
        if (!user.getPasswordHash().equals(password)) {
            throw new RuntimeException("Şifre yanlış!");
        }
        return jwtUtil.generateToken(user.getUsername());
    }
}