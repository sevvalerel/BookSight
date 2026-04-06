package com.booksight.booksight;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.controller.UserController;
import com.booksight.booksight.dto.UserDTO;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserController userController;

    private User createUser() {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("sevval");
        user.setEmail("sevval@example.com");
        user.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 0));
        return user;
    }

    @Test
    void getCurrentUser_gecerliToken_kullaniciDonmeli() {
        // ARRANGE
        User user = createUser();
        when(jwtUtil.extractUsername("valid.token.here")).thenReturn("sevval");
        when(userRepository.findByUsername("sevval")).thenReturn(Optional.of(user));

        // ACT
        ResponseEntity<UserDTO> response = userController.getCurrentUser("Bearer valid.token.here");

        // ASSERT
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());

        UserDTO dto = response.getBody();
        assertNotNull(dto);
        assertEquals(1L, dto.getUserId());
        assertEquals("sevval", dto.getUsername());
        assertEquals("sevval@example.com", dto.getEmail());
        assertNotNull(dto.getCreatedAt());
    }

    @Test
    void getCurrentUser_sifreHicDonmemeli() {
        // ARRANGE
        User user = createUser();
        when(jwtUtil.extractUsername("valid.token.here")).thenReturn("sevval");
        when(userRepository.findByUsername("sevval")).thenReturn(Optional.of(user));

        // ACT
        ResponseEntity<UserDTO> response = userController.getCurrentUser("Bearer valid.token.here");

        // ASSERT — UserDTO'da passwordHash alanı bulunmamalı
        UserDTO dto = response.getBody();
        assertNotNull(dto);
        // UserDTO sınıfında getPasswordHash() metodu olmamalı
        assertDoesNotThrow(() -> dto.getClass().getMethod("getUserId"));
        assertThrows(NoSuchMethodException.class, () -> dto.getClass().getMethod("getPasswordHash"));
    }

    @Test
    void getCurrentUser_kullaniciBulunamazsa_hataDonmeli() {
        // ARRANGE
        when(jwtUtil.extractUsername("gecersiz.token")).thenReturn("bilinmeyen");
        when(userRepository.findByUsername("bilinmeyen")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(RuntimeException.class, () ->
                userController.getCurrentUser("Bearer gecersiz.token")
        );
    }

    @Test
    void getCurrentUser_bearerPrefixiDogru_temizlenmeli() {
        // ARRANGE
        User user = createUser();
        when(jwtUtil.extractUsername("token.degeri.burada")).thenReturn("sevval");
        when(userRepository.findByUsername("sevval")).thenReturn(Optional.of(user));

        // ACT — "Bearer " prefix'i ile gönderiliyor
        ResponseEntity<UserDTO> response = userController.getCurrentUser("Bearer token.degeri.burada");

        // ASSERT — jwtUtil'e sadece token kısmı iletilmeli
        verify(jwtUtil).extractUsername("token.degeri.burada");
        assertEquals(200, response.getStatusCode().value());
    }
}
