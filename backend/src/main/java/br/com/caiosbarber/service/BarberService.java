package br.com.caiosbarber.service;

import br.com.caiosbarber.dto.BarberResponseDto;
import br.com.caiosbarber.model.Barber;
import br.com.caiosbarber.repository.BarberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BarberService {

    private final BarberRepository barberRepository;

    public BarberService(BarberRepository barberRepository) {
        this.barberRepository = barberRepository;
    }

    @Transactional(readOnly = true)
    public List<BarberResponseDto> findAllActive() {
        return barberRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(BarberResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Barber findEntityById(UUID id) {
        return barberRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Barbeiro nao encontrado ou inativo"));
    }
}
