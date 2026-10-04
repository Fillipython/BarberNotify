package br.com.barberman.service;

import br.com.barberman.dto.ClientRequestDto;
import br.com.barberman.dto.ClientResponseDto;
import br.com.barberman.model.Client;
import br.com.barberman.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public Optional<ClientResponseDto> findByPhone(String phone) {
        String cleanPhone = cleanPhone(phone);
        return clientRepository.findByPhone(cleanPhone)
                .map(ClientResponseDto::fromEntity);
    }

    @Transactional
    public ClientResponseDto createOrUpdate(ClientRequestDto dto) {
        String cleanPhone = cleanPhone(dto.getPhone());

        Client client = clientRepository.findByPhone(cleanPhone)
                .orElse(Client.builder().phone(cleanPhone).build());

        client.setName(dto.getName().trim());
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            client.setEmail(dto.getEmail().trim().toLowerCase());
        }
        if (dto.getNotificationsEnabled() != null) {
            client.setNotificationsEnabled(dto.getNotificationsEnabled());
        }

        Client saved = clientRepository.save(client);
        return ClientResponseDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Client findEntityById(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado com o ID informado"));
    }

    private String cleanPhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("\\D", "");
    }
}
