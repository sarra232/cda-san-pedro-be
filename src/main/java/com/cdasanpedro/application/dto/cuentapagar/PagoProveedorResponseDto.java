package com.cdasanpedro.application.dto.cuentapagar;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoProveedorResponseDto {

    private UUID id;
    private UUID cuentaPorPagarId;
    private BigDecimal montoPagado;
    private OffsetDateTime fechaPago;
    private String metodoPago;
    private String numeroComprobante;
    private String soporteUrlArchivo;
    private UUID usuarioId;
    private String usuarioNombre;
    private String observaciones;
    private String metadata;
    private OffsetDateTime createdAt;
}
