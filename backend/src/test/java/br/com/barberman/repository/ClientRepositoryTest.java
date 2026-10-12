package br.com.barberman.repository;

import br.com.barberman.model.Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ClientRepositoryTest {

    @Autowired
    private ClientRepository clientRepository;

    @Test
    @DisplayName("Deve buscar cliente por telefone unico")
    void testFindByPhone() {
        Client client = clientRepository.save(Client.builder()
                .name("Fernando Dias")
                .phone("11987654321")
                .email("fernando@teste.com")
                .notificationsEnabled(true)
                .build());

        Optional<Client> found = clientRepository.findByPhone("11987654321");

        assertTrue(found.isPresent());
        assertEquals("Fernando Dias", found.get().getName());
        assertEquals(client.getId(), found.get().getId());
    }

    @Test
    @DisplayName("Deve retornar vazio ao buscar telefone nao existente")
    void testFindByPhone_NotFound() {
        Optional<Client> found = clientRepository.findByPhone("11000000000");
        assertTrue(found.isEmpty());
    }
}
