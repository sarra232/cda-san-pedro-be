package com.cdasanpedro.application.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {

    private BigDecimal recaudoHoy;
    private Long vehiculosAtendidosHoy;
    private Long facturasEmitidasHoy;
    private Long alertasVencimiento;

    // Desglose por Categoría Vehicular
    private Long totalMotos;
    private Long totalLivianos;
    private Long totalPesados;
    private Long totalPublicos;

    // Gráfico de Actividad por Horas
    @Builder.Default
    private List<ActividadHorariaDto> actividadPorHoras = new ArrayList<>();
}
