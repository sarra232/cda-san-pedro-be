package com.cdasanpedro.application.dto.cliente;

import com.cdasanpedro.core.model.enums.TipoDocumento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponseDto {

    private UUID id;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private String nombresRazonSocial;
    private String direccion;
    private String celular;
    private String email;
    private LocalDate fechaNacimiento;
    private OffsetDateTime createdAt;
}
