package com.cdasanpedro.application.dto.cuentapagar;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoProveedorRequestDto {

    @NotNull(message = "El monto a pagar es obligatorio")
    @Positive(message = "El monto pagado debe ser positivo")
    private BigDecimal montoPagado;

    private OffsetDateTime fechaPago;
    private String metodoPago;
    private String numeroComprobante;
    private String soporteUrlArchivo;
    private UUID usuarioId;
    private String observaciones;
    private String metadata;
}
