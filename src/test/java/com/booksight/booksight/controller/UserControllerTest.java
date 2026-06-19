package com.booksight.booksight.controller;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.dto.*;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.UserRepository;
import com.booksight.booksight.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    @Test
    void checkin_basarili_200DonmelveDtoDonmeli() {
        User user = createUser();
        CheckinResponseDTO dto = new CheckinResponseDTO(3, 30, false);

        when(jwtUtil.extractUsername("valid.token.here")).thenReturn("sevval");
        when(userRepository.findByUsername("sevval")).thenReturn(Optional.of(user));
        when(userService.checkin(1L)).thenReturn(dto);

        ResponseEntity<?> response = userController.checkin("Bearer valid.token.here");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(dto, response.getBody());
    }

    @Test
    void checkin_bugunZatenYapilmis_400Donmeli() {
        User user = createUser();

        when(jwtUtil.extractUsername("valid.token.here")).thenReturn("sevval");
        when(userRepository.findByUsername("sevval")).thenReturn(Optional.of(user));
        when(userService.checkin(1L)).thenThrow(new RuntimeException("Bugün zaten okuma kaydettiniz"));

        ResponseEntity<?> response = userController.checkin("Bearer valid.token.here");

        assertEquals(400, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Bugün zaten okuma kaydettiniz", body.get("message"));
    }

    @Test
    void checkin_baskaBirHata_exceptionFirlatmali() {
        User user = createUser();

        when(jwtUtil.extractUsername("valid.token.here")).thenReturn("sevval");
        when(userRepository.findByUsername("sevval")).thenReturn(Optional.of(user));
        when(userService.checkin(1L)).thenThrow(new RuntimeException("Beklenmedik hata"));

        assertThrows(RuntimeException.class, () -> userController.checkin("Bearer valid.token.here"));
    }

    @Test
    void getCheckinStatus_200DonmelveDtoDonmeli() {
        User user = createUser();
        CheckinResponseDTO dto = new CheckinResponseDTO(2, 20, true);

        when(jwtUtil.extractUsername("valid.token.here")).thenReturn("sevval");
        when(userRepository.findByUsername("sevval")).thenReturn(Optional.of(user));
        when(userService.getCheckinStatus(1L)).thenReturn(dto);

        ResponseEntity<CheckinResponseDTO> response = userController.getCheckinStatus("Bearer valid.token.here");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isAlreadyCheckedIn());
    }

    @Test
    void getLeaderboard_200VeListeDonmeli() {
        LeaderboardEntryDTO entry = new LeaderboardEntryDTO(1L, "sevval", null, 100, 5);
        when(userService.getLeaderboard()).thenReturn(List.of(entry));

        ResponseEntity<List<LeaderboardEntryDTO>> response = userController.getLeaderboard();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("sevval", response.getBody().get(0).getUsername());
    }

    @Test
    void getUserPublicProfile_200VeProfilDonmeli() {
        UserPublicProfileDTO profile = new UserPublicProfileDTO(
                1L, "sevval", null, 50, 2, 5, 10L,
                List.of("Roman"), List.of());
        when(userService.getUserPublicProfile("sevval")).thenReturn(profile);

        ResponseEntity<UserPublicProfileDTO> response = userController.getUserPublicProfile("sevval");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("sevval", response.getBody().getUsername());
    }
}
