package br.com.barberman.repository;

import br.com.barberman.model.ServiceItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ServiceItemRepositoryTest {

    @Autowired
    private ServiceItemRepository serviceItemRepository;

    @Test
    @DisplayName("Deve listar servicos ativos ordenados alfabeticamente")
    void testFindByIsActiveTrueOrderByNameAsc() {
        serviceItemRepository.save(ServiceItem.builder()
                .name("Platinado")
                .priceCents(13000)
                .durationMinutes(60)
                .isActive(true)
                .build());

        serviceItemRepository.save(ServiceItem.builder()
                .name("Barba")
                .priceCents(2500)
                .durationMinutes(30)
                .isActive(true)
                .build());

        serviceItemRepository.save(ServiceItem.builder()
                .name("Antigo Servico")
                .priceCents(1000)
                .durationMinutes(15)
                .isActive(false)
                .build());

        List<ServiceItem> list = serviceItemRepository.findByIsActiveTrueOrderByNameAsc();

        assertTrue(list.stream().anyMatch(s -> s.getName().equals("Barba")));
        assertTrue(list.stream().anyMatch(s -> s.getName().equals("Platinado")));
        assertFalse(list.stream().anyMatch(s -> s.getName().equals("Antigo Servico")));

        int idxBarba = -1, idxPlatinado = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals("Barba")) idxBarba = i;
            if (list.get(i).getName().equals("Platinado")) idxPlatinado = i;
        }
        assertTrue(idxBarba < idxPlatinado);
    }

    @Test
    @DisplayName("Deve buscar apenas servicos ativos contidos na lista de IDs")
    void testFindByIdInAndIsActiveTrue() {
        ServiceItem s1 = serviceItemRepository.save(ServiceItem.builder()
                .name("Corte")
                .priceCents(3000)
                .durationMinutes(30)
                .isActive(true)
                .build());

        ServiceItem sInactive = serviceItemRepository.save(ServiceItem.builder()
                .name("Desativado")
                .priceCents(3000)
                .durationMinutes(30)
                .isActive(false)
                .build());

        List<ServiceItem> found = serviceItemRepository.findByIdInAndIsActiveTrue(List.of(s1.getId(), sInactive.getId()));

        assertEquals(1, found.size());
        assertEquals("Corte", found.get(0).getName());
    }
}
