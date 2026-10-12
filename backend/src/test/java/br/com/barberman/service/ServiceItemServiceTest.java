package br.com.barberman.service;

import br.com.barberman.dto.ServiceItemResponseDto;
import br.com.barberman.model.ServiceItem;
import br.com.barberman.repository.ServiceItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceItemServiceTest {

    @Mock
    private ServiceItemRepository serviceItemRepository;

    @InjectMocks
    private ServiceItemService serviceItemService;

    private UUID serviceId1;
    private UUID serviceId2;
    private ServiceItem s1;
    private ServiceItem s2;

    @BeforeEach
    void setUp() {
        serviceId1 = UUID.randomUUID();
        serviceId2 = UUID.randomUUID();

        s1 = ServiceItem.builder()
                .id(serviceId1)
                .name("Corte Masculino")
                .priceCents(3500)
                .durationMinutes(30)
                .isActive(true)
                .build();

        s2 = ServiceItem.builder()
                .id(serviceId2)
                .name("Barba Terapia")
                .priceCents(2500)
                .durationMinutes(30)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Deve listar todos os servicos ativos ordenados por nome")
    void testFindAllActive() {
        when(serviceItemRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(s1, s2));

        List<ServiceItemResponseDto> result = serviceItemService.findAllActive();

        assertEquals(2, result.size());
        assertEquals("Corte Masculino", result.get(0).getName());
        assertEquals("Barba Terapia", result.get(1).getName());
        verify(serviceItemRepository).findByIsActiveTrueOrderByNameAsc();
    }

    @Test
    @DisplayName("Deve buscar entidades de servico por lista de IDs")
    void testFindEntitiesByIds_Success() {
        when(serviceItemRepository.findByIdInAndIsActiveTrue(List.of(serviceId1, serviceId2)))
                .thenReturn(List.of(s1, s2));

        List<ServiceItem> result = serviceItemService.findEntitiesByIds(List.of(serviceId1, serviceId2));

        assertEquals(2, result.size());
        assertTrue(result.contains(s1));
        assertTrue(result.contains(s2));
    }

    @Test
    @DisplayName("Deve lancar excecao quando nenhum servico valido for encontrado pelos IDs")
    void testFindEntitiesByIds_Empty() {
        when(serviceItemRepository.findByIdInAndIsActiveTrue(List.of(serviceId1)))
                .thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            serviceItemService.findEntitiesByIds(List.of(serviceId1));
        });

        assertEquals("Nenhum servico valido encontrado para os IDs informados", ex.getMessage());
    }
}
