package br.com.barberman.service;

import br.com.barberman.dto.BarberResponseDto;
import br.com.barberman.model.Barber;
import br.com.barberman.repository.BarberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarberServiceTest {

    @Mock
    private BarberRepository barberRepository;

    @InjectMocks
    private BarberService barberService;

    private UUID barberId;
    private Barber activeBarber;

    @BeforeEach
    void setUp() {
        barberId = UUID.randomUUID();
        activeBarber = Barber.builder()
                .id(barberId)
                .name("Carlos Silva")
                .nickname("Mestre Carlos")
                .phone("11988880001")
                .email("carlos@barberman.com.br")
                .isAdmin(true)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Deve retornar todos os barbeiros ativos mapeados para DTO")
    void testFindAllActive() {
        when(barberRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(activeBarber));

        List<BarberResponseDto> result = barberService.findAllActive();

        assertEquals(1, result.size());
        assertEquals("Carlos Silva", result.get(0).getName());
        assertTrue(result.get(0).getIsAdmin());
        verify(barberRepository).findByIsActiveTrueOrderByNameAsc();
    }

    @Test
    @DisplayName("Deve encontrar barbeiro ativo por ID")
    void testFindEntityById_Success() {
        when(barberRepository.findByIdAndIsActiveTrue(barberId)).thenReturn(Optional.of(activeBarber));

        Barber found = barberService.findEntityById(barberId);

        assertNotNull(found);
        assertEquals(barberId, found.getId());
    }

    @Test
    @DisplayName("Deve lancar excecao quando barbeiro nao for encontrado por ID")
    void testFindEntityById_NotFound() {
        when(barberRepository.findByIdAndIsActiveTrue(barberId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            barberService.findEntityById(barberId);
        });

        assertEquals("Barbeiro nao encontrado ou inativo", ex.getMessage());
    }

    @Test
    @DisplayName("Deve encontrar barbeiro para reserva com pessimistic lock")
    void testFindEntityByIdForBooking_Success() {
        when(barberRepository.findByIdAndIsActiveTrueWithLock(barberId)).thenReturn(Optional.of(activeBarber));

        Barber found = barberService.findEntityByIdForBooking(barberId);

        assertNotNull(found);
        assertEquals(barberId, found.getId());
        verify(barberRepository).findByIdAndIsActiveTrueWithLock(barberId);
    }

    @Test
    @DisplayName("Deve lancar excecao quando barbeiro para reserva nao for encontrado")
    void testFindEntityByIdForBooking_NotFound() {
        when(barberRepository.findByIdAndIsActiveTrueWithLock(barberId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            barberService.findEntityByIdForBooking(barberId);
        });

        assertEquals("Barbeiro nao encontrado ou inativo", ex.getMessage());
    }
}
