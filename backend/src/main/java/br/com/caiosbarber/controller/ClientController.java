package br.com.caiosbarber.controller;

import br.com.caiosbarber.dto.ClientRequestDto;
import br.com.caiosbarber.dto.ClientResponseDto;
import br.com.caiosbarber.service.ClientService;
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

    @GetMapping("/search")
    public ResponseEntity<ClientResponseDto> searchByPhone(@RequestParam String phone) {
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
