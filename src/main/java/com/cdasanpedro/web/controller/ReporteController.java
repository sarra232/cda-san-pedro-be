package com.cdasanpedro.web.controller;

import com.cdasanpedro.application.dto.reporte.DashboardStatsDto;
import com.cdasanpedro.application.dto.reporte.ReporteVentasDto;
import com.cdasanpedro.application.usecase.reporte.ReporteService;
import com.cdasanpedro.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> obtenerDashboardStats() {
        DashboardStatsDto stats = reporteService.obtenerDashboardStats();
        return ResponseEntity.ok(ApiResponse.ok(stats, "Estadísticas del dashboard cargadas exitosamente"));
    }

    @GetMapping("/ventas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<ReporteVentasDto>> generarReporteVentas(
            @RequestParam(name = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(name = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        ReporteVentasDto reporte = reporteService.generarReporteVentas(fechaInicio, fechaFin);
        return ResponseEntity.ok(ApiResponse.ok(reporte, "Reporte de ventas generado exitosamente"));
    }

    @GetMapping("/ventas/export-csv")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam(name = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(name = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        String csvContent = reporteService.generarCsvVentas(fechaInicio, fechaFin);
        // Incluir BOM UTF-8 para compatibilidad nativa con Microsoft Excel en español
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);
        byte[] responseBytes = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, responseBytes, 0, bom.length);
        System.arraycopy(csvBytes, 0, responseBytes, bom.length, csvBytes.length);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "Reporte_Ventas_CDA_San_Pedro.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(responseBytes);
    }
}
