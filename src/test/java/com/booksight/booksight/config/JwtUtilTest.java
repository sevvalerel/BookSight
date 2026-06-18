package com.booksight.booksight.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test-secret-key-must-be-long-enough-for-testing-purposes");
    }

    @Test
    void generateToken_gecerliUsername_tokenUretmeli() {
        String token = jwtUtil.generateToken("sevval");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extractUsername_gecerliToken_usernameDonmeli() {
        String token = jwtUtil.generateToken("sevval");

        String username = jwtUtil.extractUsername(token);

        assertEquals("sevval", username);
    }

    @Test
    void isTokenValid_gecerliToken_trueDonmeli() {
        String token = jwtUtil.generateToken("sevval");

        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_gecersizToken_falseDonmeli() {
        assertFalse(jwtUtil.isTokenValid("gecersiz.token.string"));
    }

    @Test
    void isTokenValid_bozukToken_falseDonmeli() {
        assertFalse(jwtUtil.isTokenValid("tamamen.yanlis.bir.token.degeri"));
    }

    @Test
    void generateToken_farkliKullanicilar_farkliTokenlar() {
        String token1 = jwtUtil.generateToken("sevval");
        String token2 = jwtUtil.generateToken("kerem");

        assertNotEquals(token1, token2);
    }

    @Test
    void extractUsername_farkliKullanici_dogruUsernameDonmeli() {
        String token = jwtUtil.generateToken("kerem");

        assertEquals("kerem", jwtUtil.extractUsername(token));
    }
}