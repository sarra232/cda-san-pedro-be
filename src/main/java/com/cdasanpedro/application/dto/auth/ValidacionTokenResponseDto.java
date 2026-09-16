package com.cdasanpedro.application.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidacionTokenResponseDto {
    private boolean valido;
    private String tipo;
    private String nombresApellidos;
    private String numeroDocumento;
    private String emailEnmascarado;
    private String mensaje;
}
