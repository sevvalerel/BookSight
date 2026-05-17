package com.booksight.booksight.service;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.dto.UpdateProfileRequest;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
                .passwordHash(passwordEncoder.encode(password)) // şifreyi hashle
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    public User findByEmailOrUsername(String emailOrUsername) {
        return userRepository.findByEmail(emailOrUsername)
                .orElseGet(() -> userRepository.findByUsername(emailOrUsername)
                        .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!")));
    }
    public String loginWithUser(User user, String password, JwtUtil jwtUtil) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Şifre yanlış!");
        }
        return jwtUtil.generateToken(user.getUsername());
    }

    public String login(String email, String password, JwtUtil jwtUtil) {
        User user = findByEmailOrUsername(email);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Şifre yanlış!");
        }
        return jwtUtil.generateToken(user.getUsername());
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));
    }

    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        if (request.getUsername() != null) {
            String username = request.getUsername().trim();
            if (username.isEmpty()) {
                throw new RuntimeException("Kullanıcı adı boş olamaz!");
            }
            if (!username.equals(user.getUsername())
                    && userRepository.existsByUsernameAndUserIdNot(username, userId)) {
                throw new RuntimeException("Bu kullanıcı adı zaten alınmış!");
            }
            user.setUsername(username);
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        boolean hasNewPassword = request.getNewPassword() != null
                && !request.getNewPassword().isBlank();
        if (hasNewPassword) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new RuntimeException("Mevcut şifre gerekli!");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new RuntimeException("Mevcut şifre hatalı");
            }
            if (request.getNewPassword().equals(request.getCurrentPassword())) {
                throw new RuntimeException("Yeni şifre mevcut şifreyle aynı olamaz");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        return userRepository.save(user);
    }
}