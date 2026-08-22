package com.cdasanpedro.application.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActividadHorariaDto {

    private String name; // e.g. "08:00", "10:00", etc.
    private Long vehiculos;
    private BigDecimal ingresos;
}
