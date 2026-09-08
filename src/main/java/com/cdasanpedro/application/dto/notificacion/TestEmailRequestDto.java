package com.cdasanpedro.application.dto.notificacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestEmailRequestDto {

    @NotBlank(message = "El correo destinatario es obligatorio")
    @Email(message = "El formato del correo es inválido")
    private String email;

    private String mensaje;
}
