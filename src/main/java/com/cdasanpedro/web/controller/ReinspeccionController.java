package com.cdasanpedro.web.controller;

import com.cdasanpedro.application.dto.reinspeccion.ReinspeccionSeguimientoResponseDto;
import com.cdasanpedro.application.dto.reinspeccion.ReinspeccionVerificacionDto;
import com.cdasanpedro.application.usecase.reinspeccion.ReinspeccionService;
import com.cdasanpedro.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reinspecciones")
@RequiredArgsConstructor
public class ReinspeccionController {

    private final ReinspeccionService reinspeccionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReinspeccionSeguimientoResponseDto>>> listarSeguimientos() {
        return ResponseEntity.ok(ApiResponse.ok(reinspeccionService.listarSeguimientos(), "Seguimientos recuperados"));
    }

    @GetMapping("/verificar/{placa}")
    public ResponseEntity<ApiResponse<ReinspeccionVerificacionDto>> verificarPlaca(@PathVariable String placa) {
        return ResponseEntity.ok(ApiResponse.ok(reinspeccionService.verificarElegibilidad(placa), "Elegibilidad de reinspección verificada"));
    }

    @PostMapping("/rechazo/{ordenId}")
    public ResponseEntity<ApiResponse<ReinspeccionSeguimientoResponseDto>> registrarRechazo(
            @PathVariable UUID ordenId,
            @RequestBody(required = false) String defectosJson
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reinspeccionService.registrarRechazo(ordenId, defectosJson), "Seguimiento de 15 días iniciado"));
    }
}
