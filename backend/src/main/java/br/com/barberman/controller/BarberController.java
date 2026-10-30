package br.com.barberman.controller;

import br.com.barberman.dto.BarberResponseDto;
import br.com.barberman.service.BarberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/barbers")
public class BarberController {

    private final BarberService barberService;

    public BarberController(BarberService barberService) {
        this.barberService = barberService;
    }

    @GetMapping
    public ResponseEntity<List<BarberResponseDto>> listActiveBarbers() {
        return ResponseEntity.ok(barberService.findAllActive());
    }

    @PatchMapping("/{id}/avatar")
    public ResponseEntity<BarberResponseDto> patchBarberAvatar(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String avatarUrl = body != null ? body.get("avatarUrl") : null;
        return ResponseEntity.ok(barberService.updateAvatar(id, avatarUrl));
    }

    @PutMapping("/{id}/avatar")
    public ResponseEntity<BarberResponseDto> putBarberAvatar(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String avatarUrl = body != null ? body.get("avatarUrl") : null;
        return ResponseEntity.ok(barberService.updateAvatar(id, avatarUrl));
    }

    @DeleteMapping("/{id}/avatar")
    public ResponseEntity<BarberResponseDto> deleteBarberAvatar(@PathVariable UUID id) {
        return ResponseEntity.ok(barberService.updateAvatar(id, "REMOVED"));
    }
}
