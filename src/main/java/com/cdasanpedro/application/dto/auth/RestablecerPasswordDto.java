package com.cdasanpedro.application.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestablecerPasswordDto {

    @NotBlank(message = "El token de seguridad es obligatorio")
    private String token;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    private String newPassword;

    @NotBlank(message = "La confirmación de la contraseña es obligatoria")
    private String confirmPassword;
}
