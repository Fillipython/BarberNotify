package br.com.barberman.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientRequestDto {

    @NotBlank(message = "O nome e obrigatorio")
    @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
    private String name;

    @NotBlank(message = "O telefone e obrigatorio")
    @Size(min = 10, max = 25, message = "O telefone deve ter formato valido")
    private String phone;

    @Email(message = "O e-mail deve ser valido")
    private String email;

    @Builder.Default
    private Boolean notificationsEnabled = false;
}
