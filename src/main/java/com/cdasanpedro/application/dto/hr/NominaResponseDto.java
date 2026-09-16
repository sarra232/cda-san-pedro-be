package com.cdasanpedro.application.dto.hr;

import com.cdasanpedro.core.model.enums.EstadoNomina;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NominaResponseDto {
    private UUID id;
    private Integer periodoAnio;
    private Integer periodoMes;
    private Integer periodoQuincena;
    private String periodoDescripcion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal totalDevengado;
    private BigDecimal totalDeducciones;
    private BigDecimal totalNeto;
    private BigDecimal totalAportesPatronales;
    private BigDecimal totalProvisiones;
    private EstadoNomina estado;
    private UUID cuentaPorPagarId;
    private String observaciones;
    private List<NominaDetalleResponseDto> detalles;
    private OffsetDateTime createdAt;
}
