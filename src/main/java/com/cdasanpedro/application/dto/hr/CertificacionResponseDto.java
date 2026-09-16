package com.cdasanpedro.application.dto.hr;

import com.cdasanpedro.core.model.enums.TipoCertificacion;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificacionResponseDto {
    private UUID id;
    private UUID empleadoId;
    private String empleadoNombre;
    private String empleadoDocumento;
    private String empleadoCargo;
    private TipoCertificacion tipoCertificacion;
    private String codigoCertificado;
    private String entidadEmisora;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private long diasRestantes;
    private String colorSemaforo; // ROJO, AMARILLO, VERDE
    private String soporteUrl;
    private String estado;
    private String observaciones;
    private OffsetDateTime createdAt;
}
