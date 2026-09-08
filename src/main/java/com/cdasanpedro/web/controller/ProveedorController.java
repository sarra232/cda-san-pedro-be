package com.cdasanpedro.web.controller;

import com.cdasanpedro.application.dto.tercero.TerceroRequestDto;
import com.cdasanpedro.application.dto.tercero.TerceroResponseDto;
import com.cdasanpedro.application.usecase.tercero.TerceroService;
import com.cdasanpedro.core.model.enums.TipoRolTercero;
import com.cdasanpedro.web.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ProveedorController {

    private final TerceroService terceroService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TerceroResponseDto>>> listarProveedores() {
        List<TerceroResponseDto> list = terceroService.listarPorRol(TipoRolTercero.PROVEEDOR);
        return ResponseEntity.ok(ApiResponse.ok(list, "Proveedores recuperados exitosamente"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TerceroResponseDto>> crearProveedor(@Valid @RequestBody TerceroRequestDto request) {
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            request.setRoles(Collections.singletonList(TipoRolTercero.PROVEEDOR));
        } else if (!request.getRoles().contains(TipoRolTercero.PROVEEDOR)) {
            request.getRoles().add(TipoRolTercero.PROVEEDOR);
        }
        TerceroResponseDto response = terceroService.crear(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Proveedor registrado exitosamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TerceroResponseDto>> actualizarProveedor(
            @PathVariable UUID id,
            @Valid @RequestBody TerceroRequestDto request
    ) {
        TerceroResponseDto response = terceroService.actualizar(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Proveedor actualizado exitosamente"));
    }
}
