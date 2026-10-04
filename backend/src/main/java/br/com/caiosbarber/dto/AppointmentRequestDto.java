package br.com.caiosbarber.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRequestDto {

    @NotNull(message = "O cliente e obrigatorio")
    private UUID clientId;

    @NotNull(message = "O barbeiro e obrigatorio")
    private UUID barberId;

    @NotEmpty(message = "Ao menos um servico deve ser selecionado")
    private List<UUID> serviceIds;

    @NotNull(message = "A data e horario sao obrigatorios")
    private OffsetDateTime scheduledAt;

    private String clientNotes;
}
