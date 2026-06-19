package com.booksight.booksight.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleRuntimeException_400StatusDonmeli() {
        RuntimeException ex = new RuntimeException("Test hatası");

        ResponseEntity<?> response = handler.handleRuntimeException(ex);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void handleRuntimeException_mesajDonmeli() {
        RuntimeException ex = new RuntimeException("Kullanıcı bulunamadı!");

        ResponseEntity<?> response = handler.handleRuntimeException(ex);

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Kullanıcı bulunamadı!", body.get("message"));
        assertEquals(400, body.get("status"));
    }

    @Test
    void handleRuntimeException_emailHataMesaji_dogruDonmeli() {
        RuntimeException ex = new RuntimeException("Bu email zaten kayıtlı!");

        ResponseEntity<?> response = handler.handleRuntimeException(ex);

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Bu email zaten kayıtlı!", body.get("message"));
    }

    @Test
    void handleRuntimeException_sifreHataMesaji_dogruDonmeli() {
        RuntimeException ex = new RuntimeException("Şifre yanlış!");

        ResponseEntity<?> response = handler.handleRuntimeException(ex);

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Şifre yanlış!", body.get("message"));
    }

    @Test
    void handleResourceNotFoundException_404StatusDonmeli() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Kitap bulunamadı!");

        ResponseEntity<?> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(404, body.get("status"));
        assertEquals("Kitap bulunamadı!", body.get("message"));
    }

    @Test
    void handleForbiddenException_403StatusDonmeli() {
        ForbiddenException ex = new ForbiddenException("Bu yorumu silme yetkiniz yok!");

        ResponseEntity<?> response = handler.handleForbidden(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(403, body.get("status"));
        assertEquals("Bu yorumu silme yetkiniz yok!", body.get("message"));
    }

    @Test
    void handleException_500StatusDonmeli() {
        Exception ex = new Exception("Beklenmedik hata");

        ResponseEntity<?> response = handler.handleException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(500, body.get("status"));
    }
}