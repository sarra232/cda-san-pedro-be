package com.cdasanpedro.application.dto.cuentapagar;

import com.cdasanpedro.core.model.enums.PeriodicidadPago;
import com.cdasanpedro.core.model.enums.TipoObligacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaPorPagarRequestDto {

    @NotNull(message = "El acreedor / proveedor es obligatorio")
    private UUID acreedorTerceroId;

    private String numeroReferencia;

    @NotBlank(message = "El concepto de la obligación es obligatorio")
    private String concepto;

    @NotNull(message = "El monto total es obligatorio")
    @Positive(message = "El monto debe ser mayor a cero")
    private BigDecimal montoTotal;

    private TipoObligacion tipoObligacion;
    private PeriodicidadPago periodicidad;

    private LocalDate fechaEmision;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    private LocalDate fechaVencimiento;

    private Integer diasAvisoAnticipado;
    private String observaciones;
    private String metadata;
}
