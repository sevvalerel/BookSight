package com.booksight.booksight.config;

import com.booksight.booksight.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter Tests")
class JwtAuthFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UserRepository userRepository;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Authorization header yoksa")
    class NoAuthHeader {

        @Test
        @DisplayName("Header null ise filtre zinciri devam eder, auth set edilmez")
        void shouldContinueFilterChainWhenHeaderIsNull() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Header Bearer ile başlamıyorsa filtre zinciri devam eder")
        void shouldContinueWhenHeaderDoesNotStartWithBearer() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Token geçersizse")
    class InvalidToken {

        @Test
        @DisplayName("Geçersiz token için filtre zinciri devam eder, auth set edilmez")
        void shouldContinueWhenTokenIsInvalid() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer invalidtoken");
            when(jwtUtil.isTokenValid("invalidtoken")).thenReturn(false);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Kullanıcı bulunamazsa")
    class UserNotFound {

        @Test
        @DisplayName("Token geçerli ama kullanıcı yoksa auth set edilmez")
        void shouldContinueWhenUserDoesNotExist() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer validtoken");
            when(jwtUtil.isTokenValid("validtoken")).thenReturn(true);
            when(jwtUtil.extractUsername("validtoken")).thenReturn("unknownuser");
            when(userRepository.existsByUsername("unknownuser")).thenReturn(false);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Geçerli token ve mevcut kullanıcı")
    class ValidTokenAndUser {

        @Test
        @DisplayName("SecurityContext'e authentication set edilir")
        void shouldSetAuthenticationInSecurityContext() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer validtoken");
            when(jwtUtil.isTokenValid("validtoken")).thenReturn(true);
            when(jwtUtil.extractUsername("validtoken")).thenReturn("testuser");
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                    .isEqualTo("testuser");
        }

        @Test
        @DisplayName("Filtre zinciri devam eder")
        void shouldContinueFilterChain() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer validtoken");
            when(jwtUtil.isTokenValid("validtoken")).thenReturn(true);
            when(jwtUtil.extractUsername("validtoken")).thenReturn("testuser");
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }
}