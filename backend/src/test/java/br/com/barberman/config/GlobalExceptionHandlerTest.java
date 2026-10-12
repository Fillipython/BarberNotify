package br.com.barberman.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("IllegalArgumentException deve retornar 400 Bad Request")
    void testHandleIllegalArgument() {
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleIllegalArgument(
                new IllegalArgumentException("ID invalido")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("ID invalido", response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    @DisplayName("IllegalStateException deve retornar 409 Conflict")
    void testHandleIllegalState() {
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleIllegalState(
                new IllegalStateException("Horario indisponivel")
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Horario indisponivel", response.getBody().get("message"));
    }

    @Test
    @DisplayName("DataIntegrityViolationException deve retornar 409 Conflict com mensagem amigavel")
    void testHandleDataIntegrity() {
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleDataIntegrity(
                new DataIntegrityViolationException("Unique constraint violation")
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertTrue(response.getBody().get("message").toString().contains("acabou de ser reservado"));
    }

    @Test
    @DisplayName("CannotAcquireLockException deve retornar 409 Conflict com aviso de concorrencia")
    void testHandleCannotAcquireLock() {
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleLockAcquisition(
                new CannotAcquireLockException("Lock timeout")
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertTrue(response.getBody().get("message").toString().contains("alta concorrência"));
    }

    @Test
    @DisplayName("Exception generica deve retornar 500 Internal Server Error")
    void testHandleGenericException() {
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(
                new RuntimeException("Falha inesperada")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("Ocorreu um erro interno no servidor.", response.getBody().get("message"));
    }
}
