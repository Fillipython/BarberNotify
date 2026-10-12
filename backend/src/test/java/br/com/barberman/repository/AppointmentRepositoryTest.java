package br.com.barberman.repository;

import br.com.barberman.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private BarberRepository barberRepository;

    @Autowired
    private ClientRepository clientRepository;

    private Barber barber;
    private Client client;

    @BeforeEach
    void setUp() {
        barber = barberRepository.save(Barber.builder()
                .name("Carlos Mestre")
                .phone("11988880001")
                .isActive(true)
                .build());

        client = clientRepository.save(Client.builder()
                .name("Lucas Cliente")
                .phone("11999991111")
                .notificationsEnabled(true)
                .build());
    }

    @Test
    @DisplayName("Deve buscar agendamentos ativos por barbeiro e periodo ignorando cancelados")
    void testFindActiveByBarberAndPeriod() {
        OffsetDateTime baseTime = OffsetDateTime.of(2026, 10, 20, 10, 0, 0, 0, ZoneOffset.ofHours(-3));

        // Agendamento Confirmado
        Appointment confirmed = appointmentRepository.save(Appointment.builder()
                .barber(barber)
                .client(client)
                .scheduledAt(baseTime)
                .totalDurationMinutes(30)
                .totalPriceCents(3500)
                .status("CONFIRMED")
                .services(new ArrayList<>())
                .build());

        // Agendamento Cancelado no mesmo horario
        appointmentRepository.save(Appointment.builder()
                .barber(barber)
                .client(client)
                .scheduledAt(baseTime.plusMinutes(60))
                .totalDurationMinutes(30)
                .totalPriceCents(3500)
                .status("CANCELLED")
                .services(new ArrayList<>())
                .build());

        List<Appointment> found = appointmentRepository.findActiveByBarberAndPeriod(
                barber.getId(),
                baseTime.minusMinutes(30),
                baseTime.plusMinutes(120)
        );

        assertEquals(1, found.size());
        assertEquals(confirmed.getId(), found.get(0).getId());
        assertEquals("CONFIRMED", found.get(0).getStatus());
    }

    @Test
    @DisplayName("Deve buscar agendamentos detalhados por clientId")
    void testFindDetailedByClientId() {
        OffsetDateTime time = OffsetDateTime.of(2026, 10, 20, 14, 0, 0, 0, ZoneOffset.ofHours(-3));

        Appointment apt = appointmentRepository.save(Appointment.builder()
                .barber(barber)
                .client(client)
                .scheduledAt(time)
                .totalDurationMinutes(45)
                .totalPriceCents(5000)
                .status("CONFIRMED")
                .services(new ArrayList<>())
                .build());

        List<Appointment> found = appointmentRepository.findDetailedByClientId(client.getId());

        assertEquals(1, found.size());
        assertEquals(apt.getId(), found.get(0).getId());
        assertEquals("Carlos Mestre", found.get(0).getBarber().getName());
    }

    @Test
    @DisplayName("Deve buscar agendamentos detalhados por telefone do cliente")
    void testFindDetailedByClientPhone() {
        OffsetDateTime time = OffsetDateTime.of(2026, 10, 20, 15, 0, 0, 0, ZoneOffset.ofHours(-3));

        appointmentRepository.save(Appointment.builder()
                .barber(barber)
                .client(client)
                .scheduledAt(time)
                .totalDurationMinutes(30)
                .totalPriceCents(3000)
                .status("CONFIRMED")
                .services(new ArrayList<>())
                .build());

        List<Appointment> found = appointmentRepository.findDetailedByClientPhone("11999991111");

        assertEquals(1, found.size());
        assertEquals("Lucas Cliente", found.get(0).getClient().getName());
    }
}
