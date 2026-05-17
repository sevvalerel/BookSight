package com.booksight.booksight.service;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void register_basarili_olmali() {
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User user = userService.register("testuser", "test@test.com", "123456");

        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertEquals("test@test.com", user.getEmail());
    }

    @Test
    void register_email_kayitliysa_hata_vermeli() {
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () ->
                userService.register("testuser", "test@test.com", "123456")
        );
    }

    @Test
    void findByEmailOrUsername_kullanici_bulunamazsa_hata_vermeli() {
        when(userRepository.findByEmail("yok@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("yok@test.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                userService.findByEmailOrUsername("yok@test.com")
        );
    }
    @Test
    void register_kullanici_adi_kayitliysa_hata_vermeli() {
        when(userRepository.existsByEmail("yeni@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(RuntimeException.class, () ->
                userService.register("testuser", "yeni@test.com", "123456")
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    void findByEmailOrUsername_email_ile_bulunmali() {
        User user = User.builder()
                .username("testuser")
                .email("test@test.com")
                .build();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        User result = userService.findByEmailOrUsername("test@test.com");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void findByEmailOrUsername_username_ile_bulunmali() {
        User user = User.builder()
                .username("testuser")
                .email("test@test.com")
                .build();
        when(userRepository.findByEmail("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        User result = userService.findByEmailOrUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void loginWithUser_dogru_sifre_token_donmeli() {
        User user = User.builder()
                .username("testuser")
                .passwordHash("hashedpassword")
                .build();
        JwtUtil jwtUtil = mock(JwtUtil.class);
        when(passwordEncoder.matches("123456", "hashedpassword")).thenReturn(true);
        when(jwtUtil.generateToken("testuser")).thenReturn("mock.token");

        String token = userService.loginWithUser(user, "123456", jwtUtil);

        assertEquals("mock.token", token);
    }

    @Test
    void loginWithUser_yanlis_sifre_hata_vermeli() {
        User user = User.builder()
                .username("testuser")
                .passwordHash("hashedpassword")
                .build();
        JwtUtil jwtUtil = mock(JwtUtil.class);
        when(passwordEncoder.matches("yanlis", "hashedpassword")).thenReturn(false);

        assertThrows(RuntimeException.class, () ->
                userService.loginWithUser(user, "yanlis", jwtUtil)
        );
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void login_basarili_token_donmeli() {
        User user = User.builder()
                .username("testuser")
                .passwordHash("hashedpassword")
                .build();
        JwtUtil jwtUtil = mock(JwtUtil.class);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "hashedpassword")).thenReturn(true);
        when(jwtUtil.generateToken("testuser")).thenReturn("mock.token");

        String token = userService.login("test@test.com", "123456", jwtUtil);

        assertEquals("mock.token", token);
    }

    @Test
    void login_yanlis_sifre_hata_vermeli() {
        User user = User.builder()
                .username("testuser")
                .passwordHash("hashedpassword")
                .build();
        JwtUtil jwtUtil = mock(JwtUtil.class);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("yanlis", "hashedpassword")).thenReturn(false);

        assertThrows(RuntimeException.class, () ->
                userService.login("test@test.com", "yanlis", jwtUtil)
        );
    }
}