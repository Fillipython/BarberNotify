package br.com.barberman.dto;

import br.com.barberman.model.Appointment;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponseDto {
    private UUID id;
    private UUID clientId;
    private String clientName;
    private String clientPhone;
    private UUID barberId;
    private String barberName;
    private List<String> servicesNames;
    private Integer totalPriceCents;
    private Integer totalDurationMinutes;
    private String totalDurationLabel;
    private OffsetDateTime scheduledAt;
    private String status;

    public static AppointmentResponseDto fromEntity(Appointment apt) {
        if (apt == null) return null;

        int mins = apt.getTotalDurationMinutes();
        String label;
        if (mins >= 60) {
            int h = mins / 60;
            int m = mins % 60;
            label = m > 0 ? h + "hr " + m + "min" : h + "hr";
        } else {
            label = mins + "min";
        }

        List<String> srvNames = apt.getServices() != null
                ? apt.getServices().stream()
                    .map(s -> s.getService().getName())
                    .collect(Collectors.toList())
                : List.of();

        return AppointmentResponseDto.builder()
                .id(apt.getId())
                .clientId(apt.getClient() != null ? apt.getClient().getId() : null)
                .clientName(apt.getClient() != null ? apt.getClient().getName() : "")
                .clientPhone(apt.getClient() != null ? apt.getClient().getPhone() : "")
                .barberId(apt.getBarber() != null ? apt.getBarber().getId() : null)
                .barberName(apt.getBarber() != null ? apt.getBarber().getName() : "")
                .servicesNames(srvNames)
                .totalPriceCents(apt.getTotalPriceCents())
                .totalDurationMinutes(apt.getTotalDurationMinutes())
                .totalDurationLabel(label)
                .scheduledAt(apt.getScheduledAt())
                .status(apt.getStatus())
                .build();
    }
}
