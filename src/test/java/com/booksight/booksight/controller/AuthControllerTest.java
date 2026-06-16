package com.booksight.booksight.controller;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.entity.PasswordResetToken;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.PasswordResetTokenRepository;
import com.booksight.booksight.repository.UserRepository;
import com.booksight.booksight.service.EmailService;
import com.booksight.booksight.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository resetTokenRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController authController;

    @Test
    void register_basarili_token_ve_username_donmeli() {
        User user = User.builder()
                .username("testuser")
                .email("test@test.com")
                .build();

        when(userService.register("testuser", "test@test.com", "123456")).thenReturn(user);
        when(jwtUtil.generateToken("testuser")).thenReturn("mock.token");

        Map<String, String> body = Map.of(
                "username", "testuser",
                "email", "test@test.com",
                "password", "123456"
        );

        ResponseEntity<?> response = authController.register(body);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("mock.token", responseBody.get("token"));
        assertEquals("testuser", responseBody.get("username"));
        assertEquals("Kayıt başarılı!", responseBody.get("message"));
    }

    @Test
    void register_email_kayitliysa_exception_firlatmali() {
        when(userService.register(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Bu email zaten kayıtlı!"));

        Map<String, String> body = Map.of(
                "username", "testuser",
                "email", "kayitli@test.com",
                "password", "123456"
        );

        assertThrows(RuntimeException.class, () -> authController.register(body));
    }

    @Test
    void register_kullanici_adi_kayitliysa_exception_firlatmali() {
        when(userService.register(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Bu kullanıcı adı zaten alınmış!"));

        Map<String, String> body = Map.of(
                "username", "mevcutuser",
                "email", "yeni@test.com",
                "password", "123456"
        );

        assertThrows(RuntimeException.class, () -> authController.register(body));
    }

    @Test
    void login_basarili_token_donmeli() {
        User user = User.builder()
                .username("testuser")
                .passwordHash("hashedpassword")
                .build();

        when(userService.findByEmailOrUsername("test@test.com")).thenReturn(user);
        when(userService.loginWithUser(eq(user), eq("123456"), any(JwtUtil.class)))
                .thenReturn("mock.token");

        Map<String, String> body = Map.of(
                "email", "test@test.com",
                "password", "123456"
        );

        ResponseEntity<?> response = authController.login(body);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("mock.token", responseBody.get("token"));
        assertEquals("testuser", responseBody.get("username"));
    }

    @Test
    void login_kullanici_bulunamazsa_exception_firlatmali() {
        when(userService.findByEmailOrUsername("yok@test.com"))
                .thenThrow(new RuntimeException("Kullanıcı bulunamadı!"));

        Map<String, String> body = Map.of(
                "email", "yok@test.com",
                "password", "123456"
        );

        assertThrows(RuntimeException.class, () -> authController.login(body));
    }

    @Test
    void login_yanlis_sifre_exception_firlatmali() {
        User user = User.builder()
                .username("testuser")
                .passwordHash("hashedpassword")
                .build();

        when(userService.findByEmailOrUsername("test@test.com")).thenReturn(user);
        when(userService.loginWithUser(eq(user), eq("yanlis"), any(JwtUtil.class)))
                .thenThrow(new RuntimeException("Şifre yanlış!"));

        Map<String, String> body = Map.of(
                "email", "test@test.com",
                "password", "yanlis"
        );

        assertThrows(RuntimeException.class, () -> authController.login(body));
    }

    @Test
    void forgotPassword_kayitliEmail_basarili() {
        User user = User.builder().userId(1L).email("test@test.com").build();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(resetTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<?> response = authController.forgotPassword(Map.of("email", "test@test.com"));

        assertEquals(200, response.getStatusCode().value());
        verify(emailService).sendPasswordResetCode(eq("test@test.com"), anyString());
    }

    @Test
    void forgotPassword_kayitsizEmail_hataFirlatmali() {
        when(userRepository.findByEmail("yok@test.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                authController.forgotPassword(Map.of("email", "yok@test.com")));
    }

    @Test
    void resetPassword_gecerliKod_basarili() {
        User user = User.builder().userId(1L).passwordHash("eski").build();
        PasswordResetToken token = PasswordResetToken.builder()
                .token("123456")
                .user(user)
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        when(resetTokenRepository.findByTokenAndUsedFalse("123456")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("Yeni1234!")).thenReturn("hashedYeni");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(resetTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<?> response = authController.resetPassword(
                Map.of("code", "123456", "newPassword", "Yeni1234!"));

        assertEquals(200, response.getStatusCode().value());
        assertTrue(token.isUsed());
    }

    @Test
    void resetPassword_gecersizKod_hataFirlatmali() {
        when(resetTokenRepository.findByTokenAndUsedFalse("000000")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                authController.resetPassword(Map.of("code", "000000", "newPassword", "Yeni1234!")));
    }
}