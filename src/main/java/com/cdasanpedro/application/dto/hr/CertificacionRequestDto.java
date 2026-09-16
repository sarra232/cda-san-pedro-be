package com.cdasanpedro.application.dto.hr;

import com.cdasanpedro.core.model.enums.TipoCertificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificacionRequestDto {

    @NotNull(message = "El empleado es obligatorio")
    private UUID empleadoId;

    @NotNull(message = "El tipo de certificación es obligatorio")
    private TipoCertificacion tipoCertificacion;

    @NotBlank(message = "El código o número de certificado es obligatorio")
    private String codigoCertificado;

    private String entidadEmisora;

    @NotNull(message = "La fecha de emisión es obligatoria")
    private LocalDate fechaEmision;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    private LocalDate fechaVencimiento;

    private String soporteUrl;
    private String estado;
    private String observaciones;
}
