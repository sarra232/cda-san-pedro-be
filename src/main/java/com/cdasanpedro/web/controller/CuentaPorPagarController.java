package com.cdasanpedro.web.controller;

import com.cdasanpedro.application.dto.cuentapagar.*;
import com.cdasanpedro.application.usecase.cuentapagar.CuentaPorPagarService;
import com.cdasanpedro.web.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cuentas-por-pagar")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class CuentaPorPagarController {

    private final CuentaPorPagarService cuentaPorPagarService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CuentaPorPagarResponseDto>>> listarTodas() {
        return ResponseEntity.ok(ApiResponse.ok(cuentaPorPagarService.listarTodas(), "Cuentas por pagar recuperadas"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CuentaPorPagarResponseDto>> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(cuentaPorPagarService.obtenerPorId(id), "Cuenta por pagar encontrada"));
    }

    @GetMapping("/semaforo")
    public ResponseEntity<ApiResponse<SemaforoVencimientosDto>> obtenerSemaforo() {
        return ResponseEntity.ok(ApiResponse.ok(cuentaPorPagarService.obtenerSemaforoVencimientos(), "Semáforo de vencimientos calculado"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CuentaPorPagarResponseDto>> crear(@Valid @RequestBody CuentaPorPagarRequestDto request) {
        return ResponseEntity.ok(ApiResponse.ok(cuentaPorPagarService.crear(request), "Cuenta por pagar registrada exitosamente"));
    }

    @PostMapping("/{id}/pagos")
    public ResponseEntity<ApiResponse<CuentaPorPagarResponseDto>> registrarPago(
            @PathVariable UUID id,
            @Valid @RequestBody PagoProveedorRequestDto request,
            Authentication authentication
    ) {
        UUID usuarioId = null;
        if (request.getUsuarioId() != null) {
            usuarioId = request.getUsuarioId();
        }
        CuentaPorPagarResponseDto response = cuentaPorPagarService.registrarPago(id, request, usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Pago registrado exitosamente"));
    }
}
