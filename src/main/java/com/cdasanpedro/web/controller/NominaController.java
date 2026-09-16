package com.cdasanpedro.web.controller;

import com.cdasanpedro.application.dto.hr.LiquidacionNominaRequestDto;
import com.cdasanpedro.application.dto.hr.NominaResponseDto;
import com.cdasanpedro.application.usecase.hr.NominaService;
import com.cdasanpedro.web.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/nominas")
@RequiredArgsConstructor
public class NominaController {

    private final NominaService nominaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NominaResponseDto>>> listarNominas() {
        List<NominaResponseDto> nominas = nominaService.listarNominas();
        return ResponseEntity.ok(ApiResponse.ok(nominas, "Historial de nóminas recuperado exitosamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NominaResponseDto>> obtenerPorId(@PathVariable UUID id) {
        NominaResponseDto nomina = nominaService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.ok(nomina, "Detalle de nómina recuperado"));
    }

    @PostMapping("/liquidar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DIRECTOR_TECNICO')")
    public ResponseEntity<ApiResponse<NominaResponseDto>> liquidarNomina(
            @Valid @RequestBody LiquidacionNominaRequestDto request
    ) {
        NominaResponseDto liquidada = nominaService.liquidarNomina(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(liquidada, "Liquidación de nómina " + liquidada.getPeriodoDescripcion() + " calculada exitosamente"));
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<NominaResponseDto>> aprobarNomina(@PathVariable UUID id) {
        NominaResponseDto aprobada = nominaService.aprobarNomina(id);
        return ResponseEntity.ok(ApiResponse.ok(aprobada, "Nómina " + aprobada.getPeriodoDescripcion() + " aprobada e integrada a Tesorería"));
    }
}
