package com.cdasanpedro.application.dto.hr;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NominaDetalleResponseDto {
    private UUID id;
    private UUID empleadoId;
    private String empleadoNombre;
    private String empleadoDocumento;
    private String empleadoCargo;
    private String banco;
    private String numeroCuenta;
    private Integer diasTrabajados;
    private BigDecimal salarioBase;
    private BigDecimal sueldoDevengado;
    private BigDecimal auxilioTransporte;
    private BigDecimal horasExtras;
    private BigDecimal bonificaciones;
    private BigDecimal totalDevengado;
    private BigDecimal deduccionSalud;
    private BigDecimal deduccionPension;
    private BigDecimal otrasDeducciones;
    private BigDecimal totalDeducciones;
    private BigDecimal netoPagar;
    private BigDecimal aporteSaludPatronal;
    private BigDecimal aportePensionPatronal;
    private BigDecimal aporteArl;
    private BigDecimal parafiscalesCaja;
    private BigDecimal provisionPrima;
    private BigDecimal provisionCesantias;
    private BigDecimal provisionInteresesCesantias;
    private BigDecimal provisionVacaciones;
}
