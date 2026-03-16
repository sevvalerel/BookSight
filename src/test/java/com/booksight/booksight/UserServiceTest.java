package com.booksight.booksight;

import com.booksight.booksight.entity.User;
import com.booksight.booksight.repository.UserRepository;
import com.booksight.booksight.service.UserService;
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
    void findByEmail_kullanici_bulunamazsa_hata_vermeli() {
        when(userRepository.findByEmail("yok@test.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                userService.findByEmail("yok@test.com")
        );
    }
}