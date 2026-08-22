package com.cdasanpedro.web.controller;

import com.cdasanpedro.application.dto.ingreso.OrdenIngresoRequestDto;
import com.cdasanpedro.application.dto.ingreso.OrdenIngresoResponseDto;
import com.cdasanpedro.application.usecase.ingreso.OrdenIngresoService;
import com.cdasanpedro.core.model.enums.EstadoOrden;
import com.cdasanpedro.web.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ingresos")
@RequiredArgsConstructor
public class OrdenIngresoController {

    private final OrdenIngresoService ordenIngresoService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrdenIngresoResponseDto>> registrarIngreso(
            @Valid @RequestBody OrdenIngresoRequestDto request,
            Authentication authentication
    ) {
        UUID usuarioId = (UUID) authentication.getCredentials();
        OrdenIngresoResponseDto response = ordenIngresoService.registrarIngreso(request, usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Orden de ingreso generada exitosamente con consecutivo #" + response.getConsecutivo()));
    }

    @GetMapping("/hoy")
    public ResponseEntity<ApiResponse<List<OrdenIngresoResponseDto>>> listarIngresosHoy() {
        List<OrdenIngresoResponseDto> ingresos = ordenIngresoService.listarIngresosHoy();
        return ResponseEntity.ok(ApiResponse.ok(ingresos, "Ingresos del día cargados"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrdenIngresoResponseDto>>> listarTodos() {
        List<OrdenIngresoResponseDto> ingresos = ordenIngresoService.listarTodos();
        return ResponseEntity.ok(ApiResponse.ok(ingresos, "Órdenes de ingreso recuperadas"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrdenIngresoResponseDto>> obtenerPorId(@PathVariable UUID id) {
        OrdenIngresoResponseDto ingreso = ordenIngresoService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.ok(ingreso, "Orden de ingreso encontrada"));
    }

    @GetMapping("/listos-facturar")
    public ResponseEntity<ApiResponse<List<OrdenIngresoResponseDto>>> listarListosParaFacturar() {
        List<OrdenIngresoResponseDto> listos = ordenIngresoService.listarListosParaFacturar();
        return ResponseEntity.ok(ApiResponse.ok(listos, "Órdenes listas para cobro"));
    }

    @GetMapping("/{id}/pruebas")
    public ResponseEntity<ApiResponse<List<com.cdasanpedro.application.dto.ingreso.PruebaInspeccionResponseDto>>> listarPruebas(
            @PathVariable UUID id
    ) {
        List<com.cdasanpedro.application.dto.ingreso.PruebaInspeccionResponseDto> pruebas = ordenIngresoService.listarPruebas(id);
        return ResponseEntity.ok(ApiResponse.ok(pruebas, "Pruebas de inspección recuperadas"));
    }

    @PatchMapping("/{id}/pruebas")
    public ResponseEntity<ApiResponse<com.cdasanpedro.application.dto.ingreso.PruebaInspeccionResponseDto>> registrarResultadoPrueba(
            @PathVariable UUID id,
            @Valid @RequestBody com.cdasanpedro.application.dto.ingreso.PruebaInspeccionRequestDto request,
            Authentication authentication
    ) {
        UUID usuarioId = (UUID) authentication.getCredentials();
        com.cdasanpedro.application.dto.ingreso.PruebaInspeccionResponseDto response = ordenIngresoService.registrarResultadoPrueba(id, request, usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Resultado de prueba " + request.getTipoPrueba() + " actualizado a " + request.getEstado()));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<OrdenIngresoResponseDto>> cambiarEstado(
            @PathVariable UUID id,
            @RequestParam("estado") EstadoOrden estado,
            @RequestParam(value = "observaciones", required = false) String observaciones,
            Authentication authentication
    ) {
        UUID usuarioId = authentication != null ? (UUID) authentication.getCredentials() : null;
        OrdenIngresoResponseDto actualizada = ordenIngresoService.cambiarEstado(id, estado, observaciones, usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(actualizada, "Estado de la orden actualizado a " + estado));
    }
}
