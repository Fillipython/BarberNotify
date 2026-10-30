package br.com.barberman.controller;

import br.com.barberman.dto.AppointmentRequestDto;
import br.com.barberman.dto.AppointmentResponseDto;
import br.com.barberman.dto.TimeSlotDto;
import br.com.barberman.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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
            @RequestParam(value = "clientId", required = false) UUID clientId,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "date", required = false) String date) {
        if (date != null && !date.isBlank()) {
            return ResponseEntity.ok(appointmentService.findByDate(date));
        }
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
            @RequestParam("barberId") UUID barberId,
            @RequestParam("date") String date,
            @RequestParam(value = "duration", defaultValue = "30") int duration) {
        return ResponseEntity.ok(appointmentService.getAvailableSlots(barberId, date, duration));
    }

    @GetMapping("/day-block")
    public ResponseEntity<Map<String, Object>> getDayBlockStatus(
            @RequestParam("date") String date) {
        return ResponseEntity.ok(appointmentService.getDayBlockStatus(date));
    }

    @PostMapping("/day-block")
    public ResponseEntity<Map<String, Object>> closeDay(
            @RequestBody Map<String, Object> body) {
        String date = String.valueOf(body.getOrDefault("date", ""));
        String barberIdStr = body.get("barberId") != null ? String.valueOf(body.get("barberId")) : null;
        boolean closeShop = Boolean.parseBoolean(String.valueOf(body.getOrDefault("closeShop", "false")));
        UUID barberId = (barberIdStr != null && !barberIdStr.isBlank() && !"null".equalsIgnoreCase(barberIdStr))
                ? UUID.fromString(barberIdStr)
                : null;
        return ResponseEntity.ok(appointmentService.closeDay(date, barberId, closeShop));
    }

    @DeleteMapping("/day-block")
    public ResponseEntity<Map<String, Object>> reopenDay(
            @RequestParam("date") String date,
            @RequestParam(value = "barberId", required = false) UUID barberId,
            @RequestParam(value = "openShop", defaultValue = "false") boolean openShop) {
        return ResponseEntity.ok(appointmentService.reopenDay(date, barberId, openShop));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelAppointment(@PathVariable("id") UUID id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/by-slot")
    public ResponseEntity<Void> cancelBySlot(
            @RequestParam("barberId") UUID barberId,
            @RequestParam("date") String date,
            @RequestParam("time") String time) {
        appointmentService.cancelByBarberAndTime(barberId, date, time);
        return ResponseEntity.noContent().build();
    }
}
