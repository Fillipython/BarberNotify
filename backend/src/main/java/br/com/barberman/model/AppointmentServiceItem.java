package br.com.barberman.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "appointment_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentServiceItem {

    @EmbeddedId
    private AppointmentServiceId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("appointmentId")
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("serviceId")
    @JoinColumn(name = "service_id")
    private ServiceItem service;

    @Column(name = "price_cents_at_booking", nullable = false)
    private Integer priceCentsAtBooking;

    @Column(name = "duration_minutes_at_booking", nullable = false)
    private Integer durationMinutesAtBooking;
}
