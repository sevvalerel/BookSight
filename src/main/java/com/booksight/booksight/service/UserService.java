package com.booksight.booksight.service;

import com.booksight.booksight.config.JwtUtil;
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
}