package br.com.barberman.controller;

import br.com.barberman.dto.BarberResponseDto;
import br.com.barberman.service.BarberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
