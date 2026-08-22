package com.cdasanpedro.web.controller;

import com.cdasanpedro.application.dto.tarifa.TarifaCreateRequestDto;
import com.cdasanpedro.application.dto.tarifa.TarifaResponseDto;
import com.cdasanpedro.application.dto.tarifa.TarifaUpdateRequestDto;
import com.cdasanpedro.application.usecase.tarifa.TarifaService;
import com.cdasanpedro.core.model.enums.CategoriaVehiculo;
import com.cdasanpedro.web.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tarifas")
@RequiredArgsConstructor
public class TarifaController {

    private final TarifaService tarifaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TarifaResponseDto>>> listarTarifas(
            @RequestParam(required = false) CategoriaVehiculo categoria,
            @RequestParam(required = false) Boolean soloActivos
    ) {
        List<TarifaResponseDto> tarifas = tarifaService.listarTarifas(categoria, soloActivos);
        return ResponseEntity.ok(ApiResponse.ok(tarifas, "Catálogo de servicios recuperado exitosamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TarifaResponseDto>> obtenerPorId(@PathVariable UUID id) {
        TarifaResponseDto tarifa = tarifaService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.ok(tarifa, "Servicio encontrado"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TarifaResponseDto>> crearTarifa(
            @Valid @RequestBody TarifaCreateRequestDto request,
            Authentication authentication
    ) {
        UUID usuarioId = authentication != null ? (UUID) authentication.getCredentials() : null;
        TarifaResponseDto creada = tarifaService.crearTarifa(request, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(creada, "Servicio '" + creada.getNombreServicio() + "' creado exitosamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TarifaResponseDto>> actualizarTarifa(
            @PathVariable UUID id,
            @Valid @RequestBody TarifaUpdateRequestDto request,
            Authentication authentication
    ) {
        UUID usuarioId = authentication != null ? (UUID) authentication.getCredentials() : null;
        TarifaResponseDto actualizada = tarifaService.actualizarTarifa(id, request, usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(actualizada, "Servicio '" + actualizada.getNombreServicio() + "' actualizado exitosamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminarTarifa(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID usuarioId = authentication != null ? (UUID) authentication.getCredentials() : null;
        tarifaService.eliminarTarifa(id, usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Servicio eliminado exitosamente del catálogo oficial"));
    }
}
