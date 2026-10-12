package br.com.barberman.repository;

import br.com.barberman.model.Barber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class BarberRepositoryTest {

    @Autowired
    private BarberRepository barberRepository;

    @Test
    @DisplayName("Deve buscar barbeiros ativos ordenados por nome")
    void testFindByIsActiveTrueOrderByNameAsc() {
        barberRepository.save(Barber.builder()
                .name("Zeca Silva")
                .phone("11911111111")
                .isActive(true)
                .build());

        barberRepository.save(Barber.builder()
                .name("Alexandre Lima")
                .phone("11922222222")
                .isActive(true)
                .build());

        barberRepository.save(Barber.builder()
                .name("Bruno Inativo")
                .phone("11933333333")
                .isActive(false)
                .build());

        List<Barber> list = barberRepository.findByIsActiveTrueOrderByNameAsc();

        assertTrue(list.size() >= 2);
        assertTrue(list.stream().anyMatch(b -> b.getName().equals("Alexandre Lima")));
        assertTrue(list.stream().anyMatch(b -> b.getName().equals("Zeca Silva")));
        assertFalse(list.stream().anyMatch(b -> b.getName().equals("Bruno Inativo")));

        // Verifica ordenacao alfabetica
        int idxAlex = -1, idxZeca = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals("Alexandre Lima")) idxAlex = i;
            if (list.get(i).getName().equals("Zeca Silva")) idxZeca = i;
        }
        assertTrue(idxAlex < idxZeca);
    }

    @Test
    @DisplayName("Deve buscar por ID somente se ativo")
    void testFindByIdAndIsActiveTrue() {
        Barber active = barberRepository.save(Barber.builder()
                .name("Carlos Ativo")
                .phone("11944444444")
                .isActive(true)
                .build());

        Barber inactive = barberRepository.save(Barber.builder()
                .name("Carlos Inativo")
                .phone("11955555555")
                .isActive(false)
                .build());

        Optional<Barber> foundActive = barberRepository.findByIdAndIsActiveTrue(active.getId());
        assertTrue(foundActive.isPresent());

        Optional<Barber> foundInactive = barberRepository.findByIdAndIsActiveTrue(inactive.getId());
        assertTrue(foundInactive.isEmpty());
    }
}
