package br.com.barberman.service;

import br.com.barberman.dto.AppointmentRequestDto;
import br.com.barberman.dto.AppointmentResponseDto;
import br.com.barberman.dto.TimeSlotDto;
import br.com.barberman.model.*;
import br.com.barberman.repository.AppointmentRepository;
import br.com.barberman.repository.NotificationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ClientService clientService;
    private final BarberService barberService;
    private final ServiceItemService serviceItemService;
    private final NotificationLogRepository notificationLogRepository;
    private final WhatsAppNotificationService whatsAppNotificationService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            ClientService clientService,
            BarberService barberService,
            ServiceItemService serviceItemService,
            NotificationLogRepository notificationLogRepository) {
        this(appointmentRepository, clientService, barberService, serviceItemService, notificationLogRepository, null);
    }

    @Autowired
    public AppointmentService(
            AppointmentRepository appointmentRepository,
            ClientService clientService,
            BarberService barberService,
            ServiceItemService serviceItemService,
            NotificationLogRepository notificationLogRepository,
            @Autowired(required = false) WhatsAppNotificationService whatsAppNotificationService) {
        this.appointmentRepository = appointmentRepository;
        this.clientService = clientService;
        this.barberService = barberService;
        this.serviceItemService = serviceItemService;
        this.notificationLogRepository = notificationLogRepository;
        this.whatsAppNotificationService = whatsAppNotificationService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponseDto createAppointment(AppointmentRequestDto dto) {
        // Bloqueio pessimista exclusivo na linha do Barbeiro no banco de dados.
        // Garante atomicidade e impede condicao de corrida (race condition) entre clientes simultaneos:
        // Apenas a primeira requisicao obtem o lock e efetua a reserva;
        // Requisicoes concorrentes aguardam o lock e, ao adquirirem, detectam o conflito de horario e sao rejeitadas com HTTP 409.
        Barber barber = barberService.findEntityByIdForBooking(dto.getBarberId());
        Client client = clientService.findEntityById(dto.getClientId());
        List<ServiceItem> services = serviceItemService.findEntitiesByIds(dto.getServiceIds());

        int totalMinutes = services.stream().mapToInt(ServiceItem::getDurationMinutes).sum();
        int totalPrice = services.stream().mapToInt(ServiceItem::getPriceCents).sum();

        OffsetDateTime start = dto.getScheduledAt();
        OffsetDateTime end = start.plusMinutes(totalMinutes);

        // Verificacao rigorosa de conflito de agenda no barbeiro com lock ativo
        List<Appointment> conflicts = appointmentRepository.findActiveByBarberAndPeriod(
                barber.getId(),
                start.minusMinutes(240),
                end.plusMinutes(240)
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

            if (whatsAppNotificationService != null) {
                whatsAppNotificationService.sendAppointmentConfirmation(saved);
            }
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

        String joinedServiceNames = services.stream().map(ServiceItem::getName).collect(Collectors.joining(", "));
        Appointment target = saved != null ? saved : appointment;
        notifyBarberAdminAsync("APPOINTMENT_CREATED", target, barber, client, joinedServiceNames);

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
        Appointment saved = appointmentRepository.save(apt);
        Appointment target = saved != null ? saved : apt;
        if (target.getBarber() != null && target.getClient() != null) {
            notifyBarberAdminAsync("APPOINTMENT_CANCELLED", target, target.getBarber(), target.getClient(), "Cancelado pelo cliente");
        }
    }

    private void notifyBarberAdminAsync(String eventType, Appointment apt, Barber barber, Client client, String serviceName) {
        try {
            String extId = apt.getId() != null ? apt.getId().toString() : "";
            String cName = escapeJson(client.getName());
            String cPhone = escapeJson(client.getPhone());
            String cEmail = escapeJson(client.getEmail());
            String bId = barber.getId() != null ? barber.getId().toString() : "";
            String bName = escapeJson(barber.getName());
            String sName = escapeJson(serviceName != null && !serviceName.isBlank() ? serviceName : "Corte");
            String sched = apt.getScheduledAt() != null ? apt.getScheduledAt().toString() : OffsetDateTime.now().toString();
            int dur = apt.getTotalDurationMinutes() != null ? apt.getTotalDurationMinutes() : 30;
            int price = apt.getTotalPriceCents() != null ? apt.getTotalPriceCents() : 3000;

            String jsonPayload = String.format(
                    "{\"eventType\":\"%s\",\"externalId\":\"%s\",\"clientName\":\"%s\",\"clientPhone\":\"%s\",\"clientEmail\":\"%s\",\"barberId\":\"%s\",\"barberName\":\"%s\",\"serviceName\":\"%s\",\"scheduledAt\":\"%s\",\"durationMinutes\":%d,\"customPriceCents\":%d,\"origin\":\"PORTAL\"}",
                    eventType, extId, cName, cPhone, cEmail, bId, bName, sName, sched, dur, price
            );

            CompletableFuture.runAsync(() -> {
                String[] targets = {
                        "http://barber_admin_backend:8080/api/v1/barbernotify/webhook",
                        "http://host.docker.internal:8080/api/v1/barbernotify/webhook",
                        "http://localhost:8080/api/v1/barbernotify/webhook"
                };
                HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
                for (String url : targets) {
                    try {
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .timeout(Duration.ofSeconds(3))
                                .header("Content-Type", "application/json")
                                .header("X-BarberNotify-Secret", "barbernotify_webhook_secret_2026")
                                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                                .build();
                        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                }
            });
        } catch (Exception ignored) {
        }
    }

    private String escapeJson(String val) {
        if (val == null) return "";
        return val.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Transactional(readOnly = true)
    public List<TimeSlotDto> getAvailableSlots(UUID barberId, String dateIso, int durationMinutes) {
        LocalDate date = LocalDate.parse(dateIso);
        ZoneId zone = ZoneId.of("America/Sao_Paulo");
        OffsetDateTime startOfDay = date.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime endOfDay = date.atTime(23, 59, 59).atZone(zone).toOffsetDateTime();

        List<Appointment> dayAppointments = appointmentRepository.findActiveByBarberAndPeriod(
                barberId,
                startOfDay.minusHours(4),
                endOfDay.plusHours(4)
        );

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

            // Valida se o atendimento cabe dentro do turno de trabalho da barbearia
            LocalTime endTime = slotEnd.toLocalTime();
            boolean fitsShift = true;
            if (lt.isBefore(LocalTime.of(14, 0))) {
                if (endTime.isAfter(LocalTime.of(14, 0)) || endTime.isBefore(lt)) {
                    fitsShift = false;
                }
            } else {
                if (endTime.isAfter(LocalTime.of(18, 30)) || endTime.isBefore(lt)) {
                    fitsShift = false;
                }
            }

            if (!fitsShift) {
                result.add(new TimeSlotDto(t, false));
                continue;
            }

            // Valida conflito com agendamentos existentes (ocupacao continua de horarios)
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
