package br.com.caiosbarber.controller;

import br.com.caiosbarber.dto.AppointmentRequestDto;
import br.com.caiosbarber.dto.AppointmentResponseDto;
import br.com.caiosbarber.dto.TimeSlotDto;
import br.com.caiosbarber.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponseDto> createAppointment(@RequestBody @Valid AppointmentRequestDto dto) {
        AppointmentResponseDto response = appointmentService.createAppointment(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponseDto>> listByClient(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) String phone) {
        if (clientId != null) {
            return ResponseEntity.ok(appointmentService.findByClientId(clientId));
        }
        if (phone != null && !phone.isBlank()) {
            return ResponseEntity.ok(appointmentService.findByClientPhone(phone));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/slots")
    public ResponseEntity<List<TimeSlotDto>> getAvailableSlots(
            @RequestParam UUID barberId,
            @RequestParam String date,
            @RequestParam(defaultValue = "30") int duration) {
        return ResponseEntity.ok(appointmentService.getAvailableSlots(barberId, date, duration));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelAppointment(@PathVariable UUID id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.noContent().build();
    }
}
