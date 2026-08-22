package com.cdasanpedro.web.controller;

import com.cdasanpedro.application.dto.vehiculo.VehiculoRequestDto;
import com.cdasanpedro.application.dto.vehiculo.VehiculoResponseDto;
import com.cdasanpedro.application.usecase.vehiculo.VehiculoService;
import com.cdasanpedro.core.exception.ResourceNotFoundException;
import com.cdasanpedro.web.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {

    private final VehiculoService vehiculoService;

    @PostMapping
    public ResponseEntity<ApiResponse<VehiculoResponseDto>> registrarOActualizar(@Valid @RequestBody VehiculoRequestDto request) {
        VehiculoResponseDto response = vehiculoService.registrarOActualizar(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Vehículo procesado exitosamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehiculoResponseDto>>> listarTodos() {
        List<VehiculoResponseDto> vehiculos = vehiculoService.listarTodos();
        return ResponseEntity.ok(ApiResponse.ok(vehiculos, "Vehículos recuperados"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehiculoResponseDto>> obtenerPorId(@PathVariable UUID id) {
        VehiculoResponseDto vehiculo = vehiculoService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.ok(vehiculo, "Vehículo encontrado"));
    }

    @GetMapping("/placa/{placa}")
    public ResponseEntity<ApiResponse<VehiculoResponseDto>> buscarPorPlaca(@PathVariable String placa) {
        VehiculoResponseDto vehiculo = vehiculoService.buscarPorPlaca(placa)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró vehículo con placa: " + placa));
        return ResponseEntity.ok(ApiResponse.ok(vehiculo, "Vehículo encontrado"));
    }
}
