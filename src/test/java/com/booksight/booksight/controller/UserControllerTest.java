package com.booksight.booksight.controller;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.dto.UpdateProfileRequest;
import com.booksight.booksight.dto.UserDTO;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.UserRepository;
import com.booksight.booksight.service.UserService;
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

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User createUser() {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("sevval");
        user.setEmail("sevval@example.com");
        user.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 0));
        user.setBio("Kitap sever");
        user.setAvatarUrl("https://example.com/avatar.png");
        return user;
    }

    @Test
    void getCurrentUser_gecerliToken_kullaniciDonmeli() {
        User user = createUser();
        when(jwtUtil.extractUsername("valid.token.here")).thenReturn("sevval");
        when(userRepository.findByUsername("sevval")).thenReturn(Optional.of(user));
        when(userService.getUserById(1L)).thenReturn(user);

        ResponseEntity<UserDTO> response = userController.getCurrentUser("Bearer valid.token.here");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());

        UserDTO dto = response.getBody();
        assertNotNull(dto);
        assertEquals(1L, dto.getUserId());
        assertEquals("sevval", dto.getUsername());
        assertEquals("sevval@example.com", dto.getEmail());
        assertEquals("Kitap sever", dto.getBio());
        assertNotNull(dto.getCreatedAt());
    }

    @Test
    void getCurrentUser_sifreHicDonmemeli() {
        User user = createUser();
        when(jwtUtil.extractUsername("valid.token.here")).thenReturn("sevval");
        when(userRepository.findByUsername("sevval")).thenReturn(Optional.of(user));
        when(userService.getUserById(1L)).thenReturn(user);

        ResponseEntity<UserDTO> response = userController.getCurrentUser("Bearer valid.token.here");

        UserDTO dto = response.getBody();
        assertNotNull(dto);
        assertDoesNotThrow(() -> dto.getClass().getMethod("getUserId"));
        assertThrows(NoSuchMethodException.class, () -> dto.getClass().getMethod("getPasswordHash"));
    }

    @Test
    void getCurrentUser_kullaniciBulunamazsa_hataDonmeli() {
        when(jwtUtil.extractUsername("gecersiz.token")).thenReturn("bilinmeyen");
        when(userRepository.findByUsername("bilinmeyen")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                userController.getCurrentUser("Bearer gecersiz.token")
        );
    }

    @Test
    void updateCurrentUser_profilGuncellenmeli() {
        User user = createUser();
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setBio("Yeni bio");

        when(jwtUtil.extractUsername("valid.token.here")).thenReturn("sevval");
        when(userRepository.findByUsername("sevval")).thenReturn(Optional.of(user));
        when(userService.updateProfile(1L, request)).thenReturn(user);
        when(jwtUtil.generateToken("sevval")).thenReturn("yeni.token");

        ResponseEntity<com.booksight.booksight.dto.UpdateProfileResponse> response =
                userController.updateCurrentUser("Bearer valid.token.here", request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("yeni.token", response.getBody().getToken());
        verify(userService).updateProfile(1L, request);
    }
}
