package br.com.caiosbarber.dto;

import br.com.caiosbarber.model.Client;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientResponseDto {
    private UUID id;
    private String name;
    private String phone;
    private String email;
    private Boolean notificationsEnabled;

    public static ClientResponseDto fromEntity(Client client) {
        if (client == null) return null;
        return ClientResponseDto.builder()
                .id(client.getId())
                .name(client.getName())
                .phone(client.getPhone())
                .email(client.getEmail())
                .notificationsEnabled(client.getNotificationsEnabled())
                .build();
    }
}
