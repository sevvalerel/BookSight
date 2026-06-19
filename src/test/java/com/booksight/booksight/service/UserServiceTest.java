package com.booksight.booksight.service;

import com.booksight.booksight.config.JwtUtil;
import com.booksight.booksight.dto.CheckinResponseDTO;
import com.booksight.booksight.dto.LeaderboardEntryDTO;
import com.booksight.booksight.dto.UpdateProfileRequest;
import com.booksight.booksight.dto.UserPublicProfileDTO;
import com.booksight.booksight.entity.Book;
import com.booksight.booksight.entity.Review;
import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.ReadingStatusRepository;
import com.booksight.booksight.repository.ReviewRepository;
import com.booksight.booksight.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReadingStatusRepository readingStatusRepository;

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

    @Test
    void getUserById_bulunan_donmeli() {
        User user = User.builder().userId(1L).username("testuser").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getUserById_bulunamayan_hataFirlatmali() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.getUserById(99L));
    }

    @Test
    void updateProfile_kullaniciAdiGuncelle_basarili() {
        User user = User.builder().userId(1L).username("eskiad").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameAndUserIdNot("yeniad", 1L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("yeniad");

        User result = userService.updateProfile(1L, request);

        assertEquals("yeniad", result.getUsername());
    }

    @Test
    void updateProfile_kullaniciAdiAlinmis_hataFirlatmali() {
        User user = User.builder().userId(1L).username("eskiad").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameAndUserIdNot("alinmis", 1L)).thenReturn(true);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("alinmis");

        assertThrows(RuntimeException.class, () -> userService.updateProfile(1L, request));
    }

    @Test
    void updateProfile_bosKullaniciAdi_hataFirlatmali() {
        User user = User.builder().userId(1L).username("eskiad").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("   ");

        assertThrows(RuntimeException.class, () -> userService.updateProfile(1L, request));
    }

    @Test
    void updateProfile_sifreGuncelle_basarili() {
        User user = User.builder().userId(1L).passwordHash("hashedEski").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("EskiSifre1!", "hashedEski")).thenReturn(true);
        when(passwordEncoder.encode("YeniSifre1!")).thenReturn("hashedYeni");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setCurrentPassword("EskiSifre1!");
        request.setNewPassword("YeniSifre1!");

        User result = userService.updateProfile(1L, request);

        assertEquals("hashedYeni", result.getPasswordHash());
    }

    @Test
    void updateProfile_yanlisMevcutSifre_hataFirlatmali() {
        User user = User.builder().userId(1L).passwordHash("hashedEski").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("yanlis", "hashedEski")).thenReturn(false);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setCurrentPassword("yanlis");
        request.setNewPassword("YeniSifre1!");

        assertThrows(RuntimeException.class, () -> userService.updateProfile(1L, request));
    }

    @Test
    void updateProfile_mevcutSifreSizYeniSifre_hataFirlatmali() {
        User user = User.builder().userId(1L).passwordHash("hashedEski").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNewPassword("YeniSifre1!");

        assertThrows(RuntimeException.class, () -> userService.updateProfile(1L, request));
    }

    @Test
    void checkin_ilkKez_streak1OlmalivePuanArtmlai() {
        User user = User.builder().userId(1L).streak(0).totalPoints(0).lastCheckinDate(null).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        CheckinResponseDTO result = userService.checkin(1L);

        assertEquals(1, result.getStreak());
        assertEquals(10, result.getTotalPoints());
    }

    @Test
    void checkin_dunYapilmis_streakArtar() {
        User user = User.builder().userId(1L).streak(3).totalPoints(30)
                .lastCheckinDate(LocalDate.now().minusDays(1)).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        CheckinResponseDTO result = userService.checkin(1L);

        assertEquals(4, result.getStreak());
        assertEquals(40, result.getTotalPoints());
    }

    @Test
    void checkin_eskiTarih_streakSifirlanir() {
        User user = User.builder().userId(1L).streak(5).totalPoints(50)
                .lastCheckinDate(LocalDate.now().minusDays(3)).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        CheckinResponseDTO result = userService.checkin(1L);

        assertEquals(1, result.getStreak());
    }

    @Test
    void checkin_bugunZatenYapilmis_hataFirlatmali() {
        User user = User.builder().userId(1L).lastCheckinDate(LocalDate.now()).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class, () -> userService.checkin(1L));
    }

    @Test
    void getCheckinStatus_bugunYapilmis_trueDonmeli() {
        User user = User.builder().userId(1L).streak(2).totalPoints(20)
                .lastCheckinDate(LocalDate.now()).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        CheckinResponseDTO result = userService.getCheckinStatus(1L);

        assertTrue(result.isAlreadyCheckedIn());
    }

    @Test
    void getCheckinStatus_bugunYapilmamis_falseDonmeli() {
        User user = User.builder().userId(1L).streak(1).totalPoints(10)
                .lastCheckinDate(LocalDate.now().minusDays(1)).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        CheckinResponseDTO result = userService.getCheckinStatus(1L);

        assertFalse(result.isAlreadyCheckedIn());
    }

    @Test
    void getLeaderboard_listeDonmeli() {
        User u1 = User.builder().userId(1L).username("ali").totalPoints(100).streak(5).build();
        User u2 = User.builder().userId(2L).username("ayse").totalPoints(80).streak(3).build();
        when(userRepository.findTop20ByOrderByTotalPointsDesc()).thenReturn(List.of(u1, u2));

        List<LeaderboardEntryDTO> result = userService.getLeaderboard();

        assertEquals(2, result.size());
        assertEquals("ali", result.get(0).getUsername());
        assertEquals(100, result.get(0).getTotalPoints());
    }

    @Test
    void getUserPublicProfile_basarili_donmeli() {
        User user = User.builder().userId(1L).username("sevval")
                .totalPoints(50).streak(2).build();
        Book book = new Book();
        book.setBookId(10L);
        book.setTitle("Kitap");
        book.setGenre("Roman");
        book.setCoverUrl("http://cover.jpg");

        Review review = Review.builder()
                .reviewId(1L).user(user).book(book).rating(4)
                .reviewText("Güzel kitap").createdAt(LocalDateTime.now()).build();

        when(userRepository.findByUsername("sevval")).thenReturn(Optional.of(user));
        when(reviewRepository.findByUserIdWithBook(1L)).thenReturn(List.of(review));
        when(readingStatusRepository.countByUserUserIdAndStatus(1L, "READ")).thenReturn(1L);

        UserPublicProfileDTO result = userService.getUserPublicProfile("sevval");

        assertNotNull(result);
        assertEquals("sevval", result.getUsername());
        assertEquals(1, result.getReviewCount());
    }

    @Test
    void getUserPublicProfile_kullaniciBulunamazsa_hataFirlatmali() {
        when(userRepository.findByUsername("yok")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.getUserPublicProfile("yok"));
    }
}