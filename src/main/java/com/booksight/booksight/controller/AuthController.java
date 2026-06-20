package com.booksight.booksight.controller;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.entity.PasswordResetToken;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.PasswordResetTokenRepository;
import com.booksight.booksight.repository.UserRepository;
import com.booksight.booksight.service.EmailService;
import com.booksight.booksight.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, JwtUtil jwtUtil,
                          EmailService emailService, UserRepository userRepository,
                          PasswordResetTokenRepository resetTokenRepository,
                          PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");

        if (username == null || username.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Kullanıcı adı boş olamaz."));
        if (email == null || !email.contains("@") || !email.contains("."))
            return ResponseEntity.badRequest().body(Map.of("message", "Geçerli bir e-posta adresi girin."));
        if (password == null || password.length() < 8)
            return ResponseEntity.badRequest().body(Map.of("message", "Şifre en az 8 karakter olmalıdır."));

        User user = userService.register(username.trim(), email.trim(), password);
        String token = jwtUtil.generateToken(user.getUsername());

        // Hoş geldin maili arka planda gönder (response'u bloklamasın)
        final String finalEmail = email;
        final String finalUsername = username;
        new Thread(() -> {
            try {
                emailService.sendWelcomeEmail(finalEmail, finalUsername);
            } catch (Exception ignored) {}
        }).start();

        return ResponseEntity.ok(Map.of(
                "message", "Kayıt başarılı!",
                "username", user.getUsername(),
                "token", token
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String emailOrUsername = request.get("email");
        String password = request.get("password");

        User user = userService.findByEmailOrUsername(emailOrUsername);
        String token = userService.loginWithUser(user, password, jwtUtil);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", user.getUsername()
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Bu e-posta adresiyle kayıtlı kullanıcı bulunamadı."));

        // 6 haneli kriptografik kod oluştur
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));

        // Token kaydet (10 dk geçerli)
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(code)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        resetTokenRepository.save(resetToken);

        // Kodu maile gönder
        emailService.sendPasswordResetCode(email, code);

        return ResponseEntity.ok(Map.of(
                "message", "Şifre sıfırlama kodu e-posta adresinize gönderildi."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String newPassword = request.get("newPassword");

        PasswordResetToken resetToken = resetTokenRepository.findByTokenAndUsedFalse(code)
                .orElseThrow(() -> new RuntimeException("Geçersiz veya kullanılmış kod."));

        if (resetToken.isExpired()) {
            throw new RuntimeException("Kodun süresi dolmuş. Lütfen yeni kod talep edin.");
        }

        // Şifreyi güncelle
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Tokeni kullanılmış olarak işaretle
        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);

        return ResponseEntity.ok(Map.of(
                "message", "Şifreniz başarıyla güncellendi."
        ));
    }
}