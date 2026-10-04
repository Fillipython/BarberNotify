package br.com.barberman.service;

import br.com.barberman.dto.AppointmentRequestDto;
import br.com.barberman.dto.AppointmentResponseDto;
import br.com.barberman.dto.TimeSlotDto;
import br.com.barberman.model.*;
import br.com.barberman.repository.AppointmentRepository;
import br.com.barberman.repository.NotificationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ClientService clientService;
    private final BarberService barberService;
    private final ServiceItemService serviceItemService;
    private final NotificationLogRepository notificationLogRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            ClientService clientService,
            BarberService barberService,
            ServiceItemService serviceItemService,
            NotificationLogRepository notificationLogRepository) {
        this.appointmentRepository = appointmentRepository;
        this.clientService = clientService;
        this.barberService = barberService;
        this.serviceItemService = serviceItemService;
        this.notificationLogRepository = notificationLogRepository;
    }

    @Transactional
    public AppointmentResponseDto createAppointment(AppointmentRequestDto dto) {
        Client client = clientService.findEntityById(dto.getClientId());
        Barber barber = barberService.findEntityById(dto.getBarberId());
        List<ServiceItem> services = serviceItemService.findEntitiesByIds(dto.getServiceIds());

        int totalMinutes = services.stream().mapToInt(ServiceItem::getDurationMinutes).sum();
        int totalPrice = services.stream().mapToInt(ServiceItem::getPriceCents).sum();

        OffsetDateTime start = dto.getScheduledAt();
        OffsetDateTime end = start.plusMinutes(totalMinutes);

        // Verificacao de conflito de agenda no barbeiro
        List<Appointment> conflicts = appointmentRepository.findActiveByBarberAndPeriod(
                barber.getId(),
                start.minusMinutes(120),
                end.plusMinutes(120)
        );

        boolean hasOverlap = conflicts.stream().anyMatch(existing -> {
            OffsetDateTime existingStart = existing.getScheduledAt();
            OffsetDateTime existingEnd = existingStart.plusMinutes(existing.getTotalDurationMinutes());
            return start.isBefore(existingEnd) && end.isAfter(existingStart);
        });

        if (hasOverlap) {
            throw new IllegalStateException("O profissional escolhido ja possui agendamento neste intervalo de horario.");
        }

        Appointment appointment = Appointment.builder()
                .client(client)
                .barber(barber)
                .scheduledAt(start)
                .totalDurationMinutes(totalMinutes)
                .totalPriceCents(totalPrice)
                .status("CONFIRMED")
                .clientNotes(dto.getClientNotes())
                .build();

        List<AppointmentServiceItem> items = services.stream().map(s -> {
            AppointmentServiceItem item = AppointmentServiceItem.builder()
                    .id(new AppointmentServiceId(null, s.getId()))
                    .appointment(appointment)
                    .service(s)
                    .priceCentsAtBooking(s.getPriceCents())
                    .durationMinutesAtBooking(s.getDurationMinutes())
                    .build();
            return item;
        }).collect(Collectors.toList());

        appointment.setServices(items);
        Appointment saved = appointmentRepository.save(appointment);

        // Registrar Log de Notificacao (WhatsApp e Email de confirmacao)
        String welcomeMsg = String.format("Ola %s, seu agendamento no Barber Man foi confirmado para %s com %s.",
                client.getName(),
                start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                barber.getName());

        if (client.getPhone() != null) {
            notificationLogRepository.save(NotificationLog.builder()
                    .client(client)
                    .appointment(saved)
                    .channel("WHATSAPP")
                    .recipient(client.getPhone())
                    .title("Agendamento Confirmado - Barber Man")
                    .message(welcomeMsg)
                    .status("SENT")
                    .build());
        }

        if (client.getEmail() != null && !client.getEmail().isBlank()) {
            notificationLogRepository.save(NotificationLog.builder()
                    .client(client)
                    .appointment(saved)
                    .channel("EMAIL")
                    .recipient(client.getEmail())
                    .title("Comprovante de Agendamento - Barber Man")
                    .message(welcomeMsg)
                    .status("SENT")
                    .build());
        }

        return AppointmentResponseDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> findByClientId(UUID clientId) {
        return appointmentRepository.findDetailedByClientId(clientId).stream()
                .map(AppointmentResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> findByClientPhone(String phone) {
        String clean = phone.replaceAll("\\D", "");
        return appointmentRepository.findDetailedByClientPhone(clean).stream()
                .map(AppointmentResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelAppointment(UUID appointmentId) {
        Appointment apt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento nao encontrado"));
        apt.setStatus("CANCELLED");
        appointmentRepository.save(apt);
    }

    @Transactional(readOnly = true)
    public List<TimeSlotDto> getAvailableSlots(UUID barberId, String dateIso, int durationMinutes) {
        LocalDate date = LocalDate.parse(dateIso);
        ZoneId zone = ZoneId.of("America/Sao_Paulo");
        OffsetDateTime startOfDay = date.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime endOfDay = date.atTime(23, 59, 59).atZone(zone).toOffsetDateTime();

        List<Appointment> dayAppointments = appointmentRepository.findActiveByBarberAndPeriod(barberId, startOfDay, endOfDay);

        String[] baseTimes = {
            "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
            "12:00", "12:30", "13:00", "13:30", "15:30", "16:00",
            "16:30", "17:00", "17:30", "18:00"
        };

        int duration = durationMinutes > 0 ? durationMinutes : 30;
        List<TimeSlotDto> result = new ArrayList<>();

        for (String t : baseTimes) {
            LocalTime lt = LocalTime.parse(t);
            OffsetDateTime slotStart = date.atTime(lt).atZone(zone).toOffsetDateTime();
            OffsetDateTime slotEnd = slotStart.plusMinutes(duration);

            boolean isAvailable = dayAppointments.stream().noneMatch(existing -> {
                OffsetDateTime existingStart = existing.getScheduledAt();
                OffsetDateTime existingEnd = existingStart.plusMinutes(existing.getTotalDurationMinutes());
                return slotStart.isBefore(existingEnd) && slotEnd.isAfter(existingStart);
            });

            result.add(new TimeSlotDto(t, isAvailable));
        }

        return result;
    }
}
