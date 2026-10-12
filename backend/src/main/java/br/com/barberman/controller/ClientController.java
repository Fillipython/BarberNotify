package br.com.barberman.controller;

import br.com.barberman.dto.ClientRequestDto;
import br.com.barberman.dto.ClientResponseDto;
import br.com.barberman.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public ResponseEntity<ClientResponseDto> getByPhone(@RequestParam(value = "phone", required = false) String phone) {
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return clientService.findByPhone(phone)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<ClientResponseDto> searchByPhone(@RequestParam("phone") String phone) {
        return clientService.findByPhone(phone)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ClientResponseDto> createOrUpdate(@RequestBody @Valid ClientRequestDto dto) {
        ClientResponseDto response = clientService.createOrUpdate(dto);
        return ResponseEntity.ok(response);
    }
}
