package br.com.barberman.service;

import br.com.barberman.dto.AppointmentRequestDto;
import br.com.barberman.dto.AppointmentResponseDto;
import br.com.barberman.dto.TimeSlotDto;
import br.com.barberman.model.*;
import br.com.barberman.repository.AppointmentRepository;
import br.com.barberman.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ClientService clientService;

    @Mock
    private BarberService barberService;

    @Mock
    private ServiceItemService serviceItemService;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private Barber testBarber;
    private Client testClient;
    private ServiceItem testService1;
    private ServiceItem testService2;
    private UUID barberId;
    private UUID clientId;
    private UUID serviceId1;
    private UUID serviceId2;

    @BeforeEach
    void setUp() {
        barberId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        serviceId1 = UUID.randomUUID();
        serviceId2 = UUID.randomUUID();

        testBarber = Barber.builder()
                .id(barberId)
                .name("Carlos Silva")
                .isActive(true)
                .build();

        testClient = Client.builder()
                .id(clientId)
                .name("João Pereira")
                .phone("11999998888")
                .email("joao@teste.com")
                .notificationsEnabled(true)
                .build();

        testService1 = ServiceItem.builder()
                .id(serviceId1)
                .name("Corte")
                .priceCents(3500)
                .durationMinutes(30)
                .isActive(true)
                .build();

        testService2 = ServiceItem.builder()
                .id(serviceId2)
                .name("Barba")
                .priceCents(2500)
                .durationMinutes(30)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Deve criar agendamento com sucesso calculando duracao e preco total e registrando notificacoes")
    void testCreateAppointment_Success() {
        OffsetDateTime scheduledAt = OffsetDateTime.of(2026, 10, 15, 10, 0, 0, 0, ZoneOffset.ofHours(-3));

        AppointmentRequestDto dto = AppointmentRequestDto.builder()
                .barberId(barberId)
                .clientId(clientId)
                .serviceIds(List.of(serviceId1, serviceId2))
                .scheduledAt(scheduledAt)
                .clientNotes("Sem observações adicionais")
                .build();

        when(barberService.findEntityByIdForBooking(barberId)).thenReturn(testBarber);
        when(clientService.findEntityById(clientId)).thenReturn(testClient);
        when(serviceItemService.findEntitiesByIds(dto.getServiceIds())).thenReturn(List.of(testService1, testService2));
        when(appointmentRepository.findActiveByBarberAndPeriod(eq(barberId), any(), any())).thenReturn(List.of());

        UUID savedId = UUID.randomUUID();
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment toSave = invocation.getArgument(0);
            toSave.setId(savedId);
            return toSave;
        });

        AppointmentResponseDto response = appointmentService.createAppointment(dto);

        assertNotNull(response);
        assertEquals(savedId, response.getId());
        assertEquals(6000, response.getTotalPriceCents()); // 3500 + 2500
        assertEquals(60, response.getTotalDurationMinutes()); // 30 + 30
        assertEquals("CONFIRMED", response.getStatus());
        assertEquals("Carlos Silva", response.getBarberName());
        assertEquals("João Pereira", response.getClientName());

        // Verifica criacao das notificacoes (WhatsApp e Email)
        verify(notificationLogRepository, times(2)).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("Deve rejeitar agendamento com conflito de horario (overlap)")
    void testCreateAppointment_ConflictOverlap() {
        OffsetDateTime scheduledAt = OffsetDateTime.of(2026, 10, 15, 10, 0, 0, 0, ZoneOffset.ofHours(-3));

        AppointmentRequestDto dto = AppointmentRequestDto.builder()
                .barberId(barberId)
                .clientId(clientId)
                .serviceIds(List.of(serviceId1))
                .scheduledAt(scheduledAt)
                .build();

        when(barberService.findEntityByIdForBooking(barberId)).thenReturn(testBarber);
        when(clientService.findEntityById(clientId)).thenReturn(testClient);
        when(serviceItemService.findEntitiesByIds(dto.getServiceIds())).thenReturn(List.of(testService1));

        // Simula agendamento existente das 09:45 as 10:15 (conflito com 10:00 as 10:30)
        Appointment existingApt = Appointment.builder()
                .id(UUID.randomUUID())
                .barber(testBarber)
                .client(testClient)
                .scheduledAt(scheduledAt.minusMinutes(15))
                .totalDurationMinutes(30)
                .status("CONFIRMED")
                .build();

        when(appointmentRepository.findActiveByBarberAndPeriod(eq(barberId), any(), any()))
                .thenReturn(List.of(existingApt));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            appointmentService.createAppointment(dto);
        });

        assertTrue(ex.getMessage().contains("ja possui agendamento"));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Deve cancelar agendamento existente")
    void testCancelAppointment_Success() {
        UUID aptId = UUID.randomUUID();
        Appointment apt = Appointment.builder()
                .id(aptId)
                .status("CONFIRMED")
                .build();

        when(appointmentRepository.findById(aptId)).thenReturn(Optional.of(apt));

        appointmentService.cancelAppointment(aptId);

        assertEquals("CANCELLED", apt.getStatus());
        verify(appointmentRepository).save(apt);
    }

    @Test
    @DisplayName("Deve falhar ao cancelar agendamento inexistente")
    void testCancelAppointment_NotFound() {
        UUID aptId = UUID.randomUUID();
        when(appointmentRepository.findById(aptId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.cancelAppointment(aptId);
        });

        assertEquals("Agendamento nao encontrado", ex.getMessage());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar agendamentos por clientId")
    void testFindByClientId() {
        Appointment apt = Appointment.builder()
                .id(UUID.randomUUID())
                .barber(testBarber)
                .client(testClient)
                .scheduledAt(OffsetDateTime.now())
                .totalDurationMinutes(30)
                .totalPriceCents(3000)
                .status("CONFIRMED")
                .services(new ArrayList<>())
                .build();

        when(appointmentRepository.findDetailedByClientId(clientId)).thenReturn(List.of(apt));

        List<AppointmentResponseDto> result = appointmentService.findByClientId(clientId);
        assertEquals(1, result.size());
        assertEquals(apt.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("Deve sanitizar telefone e buscar agendamentos por telefone")
    void testFindByClientPhone() {
        Appointment apt = Appointment.builder()
                .id(UUID.randomUUID())
                .barber(testBarber)
                .client(testClient)
                .scheduledAt(OffsetDateTime.now())
                .totalDurationMinutes(30)
                .totalPriceCents(3000)
                .status("CONFIRMED")
                .services(new ArrayList<>())
                .build();

        when(appointmentRepository.findDetailedByClientPhone("11999998888")).thenReturn(List.of(apt));

        List<AppointmentResponseDto> result = appointmentService.findByClientPhone("(11) 99999-8888");
        assertEquals(1, result.size());
        verify(appointmentRepository).findDetailedByClientPhone("11999998888");
    }

    @Test
    @DisplayName("Deve calcular horarios disponiveis marcando conflitos e turnos corretamente")
    void testGetAvailableSlots() {
        // Agendamento as 10:00 com 30min de duracao
        OffsetDateTime aptTime = OffsetDateTime.of(2026, 10, 15, 10, 0, 0, 0, ZoneOffset.ofHours(-3));
        Appointment existing = Appointment.builder()
                .id(UUID.randomUUID())
                .barber(testBarber)
                .client(testClient)
                .scheduledAt(aptTime)
                .totalDurationMinutes(30)
                .status("CONFIRMED")
                .build();

        when(appointmentRepository.findActiveByBarberAndPeriod(eq(barberId), any(), any()))
                .thenReturn(List.of(existing));

        List<TimeSlotDto> slots = appointmentService.getAvailableSlots(barberId, "2026-10-15", 30);

        assertNotNull(slots);
        assertFalse(slots.isEmpty());

        // 10:00 deve estar ocupado
        TimeSlotDto slot1000 = slots.stream().filter(s -> "10:00".equals(s.getTime())).findFirst().orElseThrow();
        assertFalse(slot1000.getAvailable());

        // 09:30 deve estar disponivel
        TimeSlotDto slot0930 = slots.stream().filter(s -> "09:30".equals(s.getTime())).findFirst().orElseThrow();
        assertTrue(slot0930.getAvailable());

        // 13:30 com 60 min ultrapassa 14:00 (fim do turno da manha)
        List<TimeSlotDto> slots60Min = appointmentService.getAvailableSlots(barberId, "2026-10-15", 60);
        TimeSlotDto slot1330 = slots60Min.stream().filter(s -> "13:30".equals(s.getTime())).findFirst().orElseThrow();
        assertFalse(slot1330.getAvailable(), "Slot das 13:30 com 60min deve exceder o turno que termina as 14:00");
    }
}
