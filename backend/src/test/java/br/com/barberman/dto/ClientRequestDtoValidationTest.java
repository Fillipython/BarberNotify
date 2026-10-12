package br.com.barberman.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClientRequestDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("DTO valido com todos os campos deve passar sem violacoes")
    void testValidDto() {
        ClientRequestDto dto = ClientRequestDto.builder()
                .name("Carlos Alberto")
                .phone("11988887777")
                .email("carlos@teste.com")
                .notificationsEnabled(true)
                .build();

        Set<ConstraintViolation<ClientRequestDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Nome em branco ou muito curto deve produzir violacao")
    void testInvalidName() {
        ClientRequestDto dto = ClientRequestDto.builder()
                .name("Ab")
                .phone("11988887777")
                .build();

        Set<ConstraintViolation<ClientRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("entre 3 e 150 caracteres"));
    }

    @Test
    @DisplayName("Telefone em branco ou formato muito curto deve produzir violacao")
    void testInvalidPhone() {
        ClientRequestDto dto = ClientRequestDto.builder()
                .name("Carlos Alberto")
                .phone("123")
                .build();

        Set<ConstraintViolation<ClientRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("telefone"));
    }

    @Test
    @DisplayName("Email invalido deve produzir violacao")
    void testInvalidEmail() {
        ClientRequestDto dto = ClientRequestDto.builder()
                .name("Carlos Alberto")
                .phone("11988887777")
                .email("nao-e-um-email")
                .build();

        Set<ConstraintViolation<ClientRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("e-mail"));
    }
}
