package br.com.barberman.service;

import br.com.barberman.dto.ClientRequestDto;
import br.com.barberman.dto.ClientResponseDto;
import br.com.barberman.model.Client;
import br.com.barberman.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    private UUID clientId;
    private Client existingClient;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        existingClient = Client.builder()
                .id(clientId)
                .name("Roberto Carlos")
                .phone("11977776666")
                .email("roberto@teste.com")
                .notificationsEnabled(true)
                .build();
    }

    @Test
    @DisplayName("Deve buscar cliente por telefone sanitizando caracteres nao numericos")
    void testFindByPhone_Success() {
        when(clientRepository.findByPhone("11977776666")).thenReturn(Optional.of(existingClient));

        Optional<ClientResponseDto> result = clientService.findByPhone("(11) 97777-6666");

        assertTrue(result.isPresent());
        assertEquals("Roberto Carlos", result.get().getName());
        assertEquals("11977776666", result.get().getPhone());
    }

    @Test
    @DisplayName("Deve retornar Optional vazio quando cliente nao existir por telefone")
    void testFindByPhone_NotFound() {
        when(clientRepository.findByPhone("11977776666")).thenReturn(Optional.empty());

        Optional<ClientResponseDto> result = clientService.findByPhone("11977776666");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve criar novo cliente se nao existir no banco")
    void testCreateOrUpdate_NewClient() {
        ClientRequestDto dto = ClientRequestDto.builder()
                .name("  Novo Cliente  ")
                .phone("(11) 98888-9999")
                .email("NOVO@CLIENTE.COM")
                .notificationsEnabled(true)
                .build();

        when(clientRepository.findByPhone("11988889999")).thenReturn(Optional.empty());
        when(clientRepository.save(any(Client.class))).thenAnswer(i -> {
            Client c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        ClientResponseDto response = clientService.createOrUpdate(dto);

        assertNotNull(response);
        assertEquals("Novo Cliente", response.getName());
        assertEquals("11988889999", response.getPhone());
        assertEquals("novo@cliente.com", response.getEmail());
        assertTrue(response.getNotificationsEnabled());

        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(captor.capture());
        assertEquals("11988889999", captor.getValue().getPhone());
        assertEquals("Novo Cliente", captor.getValue().getName());
        assertEquals("novo@cliente.com", captor.getValue().getEmail());
    }

    @Test
    @DisplayName("Deve atualizar cliente existente mantendo o mesmo registro")
    void testCreateOrUpdate_ExistingClient() {
        ClientRequestDto dto = ClientRequestDto.builder()
                .name("Roberto Atualizado")
                .phone("11977776666")
                .email("roberto.novo@teste.com")
                .notificationsEnabled(false)
                .build();

        when(clientRepository.findByPhone("11977776666")).thenReturn(Optional.of(existingClient));
        when(clientRepository.save(any(Client.class))).thenAnswer(i -> i.getArgument(0));

        ClientResponseDto response = clientService.createOrUpdate(dto);

        assertNotNull(response);
        assertEquals("Roberto Atualizado", response.getName());
        assertEquals("roberto.novo@teste.com", response.getEmail());
        assertFalse(response.getNotificationsEnabled());
        assertEquals(clientId, response.getId());
    }

    @Test
    @DisplayName("Deve buscar entidade cliente por ID")
    void testFindEntityById_Success() {
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(existingClient));

        Client result = clientService.findEntityById(clientId);

        assertNotNull(result);
        assertEquals(clientId, result.getId());
    }

    @Test
    @DisplayName("Deve lancar excecao ao buscar entidade cliente inexistente por ID")
    void testFindEntityById_NotFound() {
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            clientService.findEntityById(clientId);
        });

        assertEquals("Cliente nao encontrado com o ID informado", ex.getMessage());
    }
}
