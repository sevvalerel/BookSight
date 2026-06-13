package com.booksight.booksight.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    @Mock
    private JwtAuthFilter jwtAuthFilter;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Nested
    @DisplayName("passwordEncoder()")
    class PasswordEncoderTests {

        @Test
        @DisplayName("BCryptPasswordEncoder döner")
        void shouldReturnBCryptPasswordEncoder() {
            PasswordEncoder encoder = securityConfig.passwordEncoder();

            assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        }

        @Test
        @DisplayName("Şifreyi encode eder")
        void shouldEncodePassword() {
            PasswordEncoder encoder = securityConfig.passwordEncoder();
            String encoded = encoder.encode("testpassword");

            assertThat(encoded).isNotEqualTo("testpassword");
            assertThat(encoded).startsWith("$2a$");
        }

        @Test
        @DisplayName("Encode edilen şifre doğrulanabilir")
        void shouldMatchEncodedPassword() {
            PasswordEncoder encoder = securityConfig.passwordEncoder();
            String raw = "testpassword";
            String encoded = encoder.encode(raw);

            assertThat(encoder.matches(raw, encoded)).isTrue();
        }

        @Test
        @DisplayName("Yanlış şifre eşleşmez")
        void shouldNotMatchWrongPassword() {
            PasswordEncoder encoder = securityConfig.passwordEncoder();
            String encoded = encoder.encode("correctpassword");

            assertThat(encoder.matches("wrongpassword", encoded)).isFalse();
        }

        @Test
        @DisplayName("Aynı şifre iki kez encode edilince farklı hash üretir")
        void shouldProduceDifferentHashesForSamePassword() {
            PasswordEncoder encoder = securityConfig.passwordEncoder();
            String encoded1 = encoder.encode("samepassword");
            String encoded2 = encoder.encode("samepassword");

            assertThat(encoded1).isNotEqualTo(encoded2);
        }
    }
}