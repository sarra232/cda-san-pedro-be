package com.cdasanpedro.application.dto.reporte;

import com.cdasanpedro.application.dto.factura.FacturaResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteVentasDto {

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal totalRecaudado;
    private Long totalVehiculos;
    private Long totalFacturas;

    @Builder.Default
    private Map<String, Long> vehiculosPorCategoria = new HashMap<>();

    @Builder.Default
    private Map<String, BigDecimal> ingresosPorMetodoPago = new HashMap<>();

    @Builder.Default
    private List<FacturaResponseDto> facturas = new ArrayList<>();
}
