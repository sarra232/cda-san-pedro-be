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
public class SolicitudRecuperacionDto {

    @NotBlank(message = "Debe proporcionar su número de documento o correo electrónico registrado")
    private String identificador;
}
