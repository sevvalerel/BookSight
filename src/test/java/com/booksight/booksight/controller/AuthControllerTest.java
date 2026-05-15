package com.booksight.booksight.controller;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

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
}