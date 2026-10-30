package br.com.barberman.service;

import br.com.barberman.dto.AppointmentRequestDto;
import br.com.barberman.dto.AppointmentResponseDto;
import br.com.barberman.dto.TimeSlotDto;
import br.com.barberman.model.*;
import br.com.barberman.repository.AppointmentRepository;
import br.com.barberman.repository.NotificationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

    private static final ZoneId ZONE_BR = ZoneId.of("America/Sao_Paulo");

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

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanupLegacyTestSlotsOnStartup() {
        try {
            OffsetDateTime oct30Start = LocalDate.of(2026, 10, 30).atStartOfDay(ZONE_BR).toOffsetDateTime();
            OffsetDateTime oct30Noon = LocalDate.of(2026, 10, 30).atTime(12, 0).atZone(ZONE_BR).toOffsetDateTime();
            List<Appointment> legacyOct30 = appointmentRepository.findActiveInPeriod(oct30Start, oct30Noon);
            for (Appointment apt : legacyOct30) {
                if (apt.getClientNotes() == null || !apt.getClientNotes().contains("AGENDA_FECHADA")) {
                    apt.setStatus("CANCELLED");
                    appointmentRepository.save(apt);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponseDto createAppointment(AppointmentRequestDto dto) {
        Barber barber = barberService.findEntityByIdForBooking(dto.getBarberId());
        Client client = clientService.findEntityById(dto.getClientId());
        List<ServiceItem> services = serviceItemService.findEntitiesByIds(dto.getServiceIds());

        int totalMinutes = services.stream().mapToInt(ServiceItem::getDurationMinutes).sum();
        int totalPrice = services.stream().mapToInt(ServiceItem::getPriceCents).sum();

        OffsetDateTime start = dto.getScheduledAt();
        OffsetDateTime end = start.plusMinutes(30);

        LocalDate targetDate = start.atZoneSameInstant(ZONE_BR).toLocalDate();
        OffsetDateTime startOfDay = targetDate.atStartOfDay(ZONE_BR).toOffsetDateTime();
        OffsetDateTime endOfDay = targetDate.atTime(23, 59, 59).atZone(ZONE_BR).toOffsetDateTime();

        List<Appointment> conflicts = appointmentRepository.findActiveByBarberAndPeriod(
                barber.getId(),
                startOfDay,
                endOfDay
        );

        boolean dayClosed = conflicts.stream().anyMatch(existing ->
                (existing.getClientNotes() != null && existing.getClientNotes().contains("AGENDA_FECHADA"))
                || (existing.getTotalDurationMinutes() != null && existing.getTotalDurationMinutes() >= 480)
        );
        if (dayClosed) {
            throw new IllegalStateException("A agenda deste profissional encontra-se fechada nesta data.");
        }

        boolean hasOverlap = conflicts.stream().anyMatch(existing -> {
            OffsetDateTime existingStart = existing.getScheduledAt();
            OffsetDateTime existingEnd = existingStart.plusMinutes(30);
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

        List<AppointmentServiceItem> items = services.stream().map(s -> AppointmentServiceItem.builder()
                .id(new AppointmentServiceId(null, s.getId()))
                .appointment(appointment)
                .service(s)
                .priceCentsAtBooking(s.getPriceCents())
                .durationMinutesAtBooking(s.getDurationMinutes())
                .build()).collect(Collectors.toList());

        appointment.setServices(items);
        Appointment saved = appointmentRepository.save(appointment);

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
                .filter(a -> a.getClientNotes() == null || !a.getClientNotes().contains("AGENDA_FECHADA"))
                .map(AppointmentResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> findByClientPhone(String phone) {
        String clean = phone.replaceAll("\\D", "");
        return appointmentRepository.findDetailedByClientPhone(clean).stream()
                .filter(a -> a.getClientNotes() == null || !a.getClientNotes().contains("AGENDA_FECHADA"))
                .map(AppointmentResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> findByDate(String dateIso) {
        LocalDate date = LocalDate.parse(dateIso);
        OffsetDateTime startOfDay = date.atStartOfDay(ZONE_BR).toOffsetDateTime();
        OffsetDateTime endOfDay = date.atTime(23, 59, 59).atZone(ZONE_BR).toOffsetDateTime();
        return appointmentRepository.findActiveInPeriod(startOfDay, endOfDay).stream()
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

    @Transactional
    public void cancelByBarberAndTime(UUID barberId, String dateIso, String timeStr) {
        LocalDate date = LocalDate.parse(dateIso);
        LocalTime time = LocalTime.parse(timeStr.length() > 5 ? timeStr.substring(0, 5) : timeStr);
        OffsetDateTime targetStart = date.atTime(time).atZone(ZONE_BR).toOffsetDateTime();
        List<Appointment> matches = appointmentRepository.findActiveByBarberAndPeriod(
                barberId,
                targetStart.minusMinutes(5),
                targetStart.plusMinutes(5)
        );
        for (Appointment apt : matches) {
            apt.setStatus("CANCELLED");
            appointmentRepository.save(apt);
        }
    }

    @Transactional
    public Map<String, Object> closeDay(String dateIso, UUID barberId, boolean closeShop) {
        LocalDate date = LocalDate.parse(dateIso);
        OffsetDateTime blockStart = date.atTime(8, 0).atZone(ZONE_BR).toOffsetDateTime();
        OffsetDateTime startOfDay = date.atStartOfDay(ZONE_BR).toOffsetDateTime();
        OffsetDateTime endOfDay = date.atTime(23, 59, 59).atZone(ZONE_BR).toOffsetDateTime();

        Client systemClient = clientService.findOrCreateSystemClient();
        List<Barber> targetBarbers = (closeShop || barberId == null)
                ? barberService.findAllActiveEntities()
                : List.of(barberService.findEntityById(barberId));

        String noteTag = closeShop ? "AGENDA_FECHADA_LOJA" : "AGENDA_FECHADA_BARBEIRO";

        for (Barber barber : targetBarbers) {
            List<Appointment> existingOnDay = appointmentRepository.findActiveByBarberAndPeriod(
                    barber.getId(), startOfDay, endOfDay);

            Optional<Appointment> existingBlock = existingOnDay.stream()
                    .filter(a -> (a.getClientNotes() != null && a.getClientNotes().contains("AGENDA_FECHADA"))
                            || a.getScheduledAt().toInstant().equals(blockStart.toInstant()))
                    .findFirst();

            if (existingBlock.isPresent()) {
                Appointment blk = existingBlock.get();
                blk.setStatus("CONFIRMED");
                blk.setTotalDurationMinutes(720);
                blk.setTotalPriceCents(0);
                blk.setClientNotes(noteTag);
                appointmentRepository.save(blk);
            } else {
                Appointment blk = Appointment.builder()
                        .client(systemClient)
                        .barber(barber)
                        .scheduledAt(blockStart)
                        .totalDurationMinutes(720)
                        .totalPriceCents(0)
                        .status("CONFIRMED")
                        .clientNotes(noteTag)
                        .build();
                appointmentRepository.save(blk);
            }
        }

        return getDayBlockStatus(dateIso);
    }

    @Transactional
    public Map<String, Object> reopenDay(String dateIso, UUID barberId, boolean openShop) {
        LocalDate date = LocalDate.parse(dateIso);
        OffsetDateTime startOfDay = date.atStartOfDay(ZONE_BR).toOffsetDateTime();
        OffsetDateTime endOfDay = date.atTime(23, 59, 59).atZone(ZONE_BR).toOffsetDateTime();

        List<Appointment> activeOnDay = appointmentRepository.findActiveInPeriod(startOfDay, endOfDay);
        for (Appointment apt : activeOnDay) {
            boolean isBlock = (apt.getClientNotes() != null && apt.getClientNotes().contains("AGENDA_FECHADA"))
                    || (apt.getTotalDurationMinutes() != null && apt.getTotalDurationMinutes() >= 480);
            if (!isBlock) continue;

            if (openShop || barberId == null || (apt.getBarber() != null && apt.getBarber().getId().equals(barberId))) {
                apt.setStatus("CANCELLED");
                appointmentRepository.save(apt);
            }
        }

        return getDayBlockStatus(dateIso);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDayBlockStatus(String dateIso) {
        LocalDate date = LocalDate.parse(dateIso);
        OffsetDateTime startOfDay = date.atStartOfDay(ZONE_BR).toOffsetDateTime();
        OffsetDateTime endOfDay = date.atTime(23, 59, 59).atZone(ZONE_BR).toOffsetDateTime();

        List<Appointment> activeOnDay = appointmentRepository.findActiveInPeriod(startOfDay, endOfDay);
        Set<String> closedBarberIds = new LinkedHashSet<>();
        boolean shopClosedByMaster = false;

        for (Appointment apt : activeOnDay) {
            boolean isBlock = (apt.getClientNotes() != null && apt.getClientNotes().contains("AGENDA_FECHADA"))
                    || (apt.getTotalDurationMinutes() != null && apt.getTotalDurationMinutes() >= 480);
            if (isBlock && apt.getBarber() != null) {
                closedBarberIds.add(apt.getBarber().getId().toString());
                if (apt.getClientNotes() != null && apt.getClientNotes().contains("AGENDA_FECHADA_LOJA")) {
                    shopClosedByMaster = true;
                }
            }
        }

        List<Barber> allActive = barberService.findAllActiveEntities();
        boolean allClosed = !allActive.isEmpty() && allActive.stream()
                .allMatch(b -> closedBarberIds.contains(b.getId().toString()));

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("date", dateIso);
        status.put("shopClosed", allClosed || shopClosedByMaster);
        status.put("shopClosedByMaster", shopClosedByMaster);
        status.put("closedBarberIds", new ArrayList<>(closedBarberIds));
        return status;
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
                        "http://barber_admin_backend:8080/api/v1/barbernotify/sync",
                        "http://host.docker.internal:8080/api/v1/barbernotify/sync",
                        "http://localhost:8080/api/v1/barbernotify/sync"
                };
                HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
                for (String url : targets) {
                    try {
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .timeout(Duration.ofSeconds(3))
                                .header("Content-Type", "application/json")
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
        OffsetDateTime startOfDay = date.atStartOfDay(ZONE_BR).toOffsetDateTime();
        OffsetDateTime endOfDay = date.atTime(23, 59, 59).atZone(ZONE_BR).toOffsetDateTime();

        List<Appointment> dayAppointments = appointmentRepository.findActiveByBarberAndPeriod(
                barberId,
                startOfDay,
                endOfDay
        );

        String[] baseTimes = {
            "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
            "12:00", "12:30", "13:00", "13:30", "15:30", "16:00",
            "16:30", "17:00", "17:30", "18:00"
        };

        boolean isDayClosedForBarber = dayAppointments.stream().anyMatch(existing ->
                (existing.getClientNotes() != null && existing.getClientNotes().contains("AGENDA_FECHADA"))
                || (existing.getTotalDurationMinutes() != null && existing.getTotalDurationMinutes() >= 480)
        );

        if (isDayClosedForBarber) {
            List<TimeSlotDto> closedResult = new ArrayList<>();
            for (String t : baseTimes) {
                closedResult.add(new TimeSlotDto(t, false));
            }
            return closedResult;
        }

        List<TimeSlotDto> result = new ArrayList<>();

        for (String t : baseTimes) {
            LocalTime lt = LocalTime.parse(t);
            OffsetDateTime slotStart = date.atTime(lt).atZone(ZONE_BR).toOffsetDateTime();
            OffsetDateTime slotEnd = slotStart.plusMinutes(30);

            // Verifica conflito apenas contra agendamentos reais existentes naquele slot especifico
            boolean isAvailable = dayAppointments.stream().noneMatch(existing -> {
                OffsetDateTime existingStart = existing.getScheduledAt();
                OffsetDateTime existingEnd = existingStart.plusMinutes(30);
                return slotStart.isBefore(existingEnd) && slotEnd.isAfter(existingStart);
            });

            result.add(new TimeSlotDto(t, isAvailable));
        }

        return result;
    }
}
