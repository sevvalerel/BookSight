package com.booksight.booksight.system;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("system")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Sistem Testleri - Canlı Render Ortamı")
class BookSightSystemTest {

    private static final String BASE_URL = "https://booksight.onrender.com";
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final String TEST_USERNAME = "sysuser_" + System.currentTimeMillis();
    private static final String TEST_EMAIL = TEST_USERNAME + "@systemtest.com";
    private static final String TEST_PASSWORD = "SystemTest1234!";

    private static String token;
    private static Long bookId;
    private static Long reviewId;

    // ── ST-01 ────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("ST-01: Kullanıcı kaydı başarılı olmalı")
    void kullanici_kaydi_basarili() {
        HttpHeaders headers = jsonHeaders();
        Map<String, String> body = Map.of(
                "username", TEST_USERNAME,
                "email", TEST_EMAIL,
                "password", TEST_PASSWORD
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/api/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody().get("token"));
    }

    // ── ST-02 ────────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("ST-02: Geçerli bilgilerle giriş yapılabilmeli")
    void gecerli_giris_token_donmeli() {
        HttpHeaders headers = jsonHeaders();
        Map<String, String> body = Map.of(
                "email", TEST_EMAIL,
                "password", TEST_PASSWORD
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        assertEquals(200, response.getStatusCode().value());
        token = (String) response.getBody().get("token");
        assertNotNull(token);
    }

    // ── ST-03 ────────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("ST-03: Yanlış şifreyle giriş reddedilmeli")
    void yanlis_sifre_giris_reddedilmeli() {
        HttpHeaders headers = jsonHeaders();
        Map<String, String> body = Map.of(
                "email", TEST_EMAIL,
                "password", "YanlisPass999!"
        );

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () ->
                restTemplate.exchange(
                        BASE_URL + "/api/auth/login",
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        Map.class
                )
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    // ── ST-04 ────────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("ST-04: Kitap listesi token olmadan alınabilmeli")
    void kitap_listesi_public_erisim() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                BASE_URL + "/api/books",
                Map.class
        );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody().get("content"));
    }

    // ── ST-05 ────────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("ST-05: Yeni kitap eklenebilmeli")
    void kitap_ekleme_basarili() {
        HttpHeaders headers = authHeaders();
        Map<String, Object> body = Map.of(
                "title", "Sistem Test Kitabı",
                "author", "Test Yazar",
                "isbn", "9999" + System.currentTimeMillis() % 1000000000L,
                "description", "Sistem testi için eklenen kitap"
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/api/books",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        assertEquals(200, response.getStatusCode().value());
        bookId = Long.valueOf(response.getBody().get("bookId").toString());
        assertNotNull(bookId);
    }

    // ── ST-06 ────────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("ST-06: Kitaba yorum eklenebilmeli")
    void yorum_ekleme_basarili() {
        assertNotNull(token, "ST-02 önce çalışmalı");
        assertNotNull(bookId, "ST-05 önce çalışmalı");

        HttpHeaders headers = authHeaders();
        Map<String, Object> body = Map.of(
                "bookId", bookId,
                "reviewText", "Bu kitap sistem testi kapsamında değerlendirilmiştir. " +
                        "Yazarın anlatım dili oldukça akıcı ve etkileyiciydi.",
                "rating", 4
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/api/reviews",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        assertEquals(200, response.getStatusCode().value());
        reviewId = Long.valueOf(response.getBody().get("reviewId").toString());
        assertNotNull(reviewId);
    }

    // ── ST-07 ────────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("ST-07: Kişiselleştirilmiş öneri alınabilmeli")
    void oneri_listesi_alinabilmeli() {
        assertNotNull(token, "ST-02 önce çalışmalı");

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/api/recommendations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody().get("recommendations"));
    }

    // ── ST-08 ────────────────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("ST-08: Token olmadan korumalı endpoint erişim reddedilmeli")
    void token_olmadan_erisim_reddedilmeli() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () ->
                restTemplate.exchange(
                        BASE_URL + "/api/recommendations",
                        HttpMethod.GET,
                        new HttpEntity<>(jsonHeaders()),
                        Map.class
                )
        );

        assertTrue(ex.getStatusCode().value() == 403 || ex.getStatusCode().value() == 401);
    }

    // ── ST-09 ────────────────────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("ST-09: Liderlik tablosu herkese açık olmalı")
    void liderlik_tablosu_public_erisim() {
        ResponseEntity<?> response = restTemplate.getForEntity(
                BASE_URL + "/api/users/leaderboard",
                Object.class
        );

        assertEquals(200, response.getStatusCode().value());
    }

    // ── Yardımcı metodlar ────────────────────────────────────────────────────

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = jsonHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }
}
