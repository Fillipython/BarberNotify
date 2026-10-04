package br.com.caiosbarber.model;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AppointmentServiceId implements Serializable {
    private UUID appointmentId;
    private UUID serviceId;
}
