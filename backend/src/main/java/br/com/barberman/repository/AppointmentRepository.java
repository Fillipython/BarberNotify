package br.com.barberman.repository;

import br.com.barberman.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Query("SELECT a FROM Appointment a " +
           "JOIN FETCH a.barber b " +
           "LEFT JOIN FETCH a.services s " +
           "LEFT JOIN FETCH s.service " +
           "WHERE a.client.id = :clientId " +
           "ORDER BY a.scheduledAt DESC")
    List<Appointment> findDetailedByClientId(@Param("clientId") UUID clientId);

    @Query("SELECT a FROM Appointment a " +
           "JOIN FETCH a.barber b " +
           "LEFT JOIN FETCH a.services s " +
           "LEFT JOIN FETCH s.service " +
           "WHERE a.client.phone = :phone " +
           "ORDER BY a.scheduledAt DESC")
    List<Appointment> findDetailedByClientPhone(@Param("phone") String phone);

    @Query("SELECT a FROM Appointment a " +
           "WHERE a.barber.id = :barberId " +
           "AND a.status <> 'CANCELLED' " +
           "AND a.scheduledAt >= :start " +
           "AND a.scheduledAt <= :end")
    List<Appointment> findActiveByBarberAndPeriod(
            @Param("barberId") UUID barberId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);
}
