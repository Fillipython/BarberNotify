package br.com.barberman.dto;

import br.com.barberman.model.Barber;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarberResponseDto {
    private UUID id;
    private String name;
    private String nickname;
    private String phone;
    private String email;
    private String avatarUrl;
    private String bio;
    private Boolean isAdmin;

    public static BarberResponseDto fromEntity(Barber barber) {
        if (barber == null) return null;
        return BarberResponseDto.builder()
                .id(barber.getId())
                .name(barber.getName())
                .nickname(barber.getNickname())
                .phone(barber.getPhone())
                .email(barber.getEmail())
                .avatarUrl(barber.getAvatarUrl())
                .bio(barber.getBio())
                .isAdmin(barber.getIsAdmin())
                .build();
    }
}
