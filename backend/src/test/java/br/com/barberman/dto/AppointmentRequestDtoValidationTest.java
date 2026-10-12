package br.com.barberman.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentRequestDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("DTO valido nao deve produzir violacoes")
    void testValidDto() {
        AppointmentRequestDto dto = AppointmentRequestDto.builder()
                .clientId(UUID.randomUUID())
                .barberId(UUID.randomUUID())
                .serviceIds(List.of(UUID.randomUUID()))
                .scheduledAt(OffsetDateTime.now())
                .clientNotes("Observacao teste")
                .build();

        Set<ConstraintViolation<AppointmentRequestDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("DTO com campos nulos obrigatorios deve produzir violacoes")
    void testMissingRequiredFields() {
        AppointmentRequestDto dto = AppointmentRequestDto.builder().build();

        Set<ConstraintViolation<AppointmentRequestDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());

        assertEquals(4, violations.size(), "Deve conter 4 violacoes: clientId, barberId, serviceIds e scheduledAt");
    }

    @Test
    @DisplayName("DTO com lista de servicos vazia deve produzir violacao")
    void testEmptyServicesList() {
        AppointmentRequestDto dto = AppointmentRequestDto.builder()
                .clientId(UUID.randomUUID())
                .barberId(UUID.randomUUID())
                .serviceIds(List.of())
                .scheduledAt(OffsetDateTime.now())
                .build();

        Set<ConstraintViolation<AppointmentRequestDto>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("servico"));
    }
}
