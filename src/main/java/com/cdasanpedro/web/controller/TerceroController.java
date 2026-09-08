package com.cdasanpedro.web.controller;

import com.cdasanpedro.application.dto.tercero.TerceroRequestDto;
import com.cdasanpedro.application.dto.tercero.TerceroResponseDto;
import com.cdasanpedro.application.usecase.tercero.TerceroService;
import com.cdasanpedro.core.model.enums.TipoRolTercero;
import com.cdasanpedro.web.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/terceros")
@RequiredArgsConstructor
public class TerceroController {

    private final TerceroService terceroService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TerceroResponseDto>>> listar(
            @RequestParam(name = "rol", required = false) TipoRolTercero rol,
            @RequestParam(name = "query", required = false) String query
    ) {
        List<TerceroResponseDto> list;
        if (query != null && !query.isBlank()) {
            list = terceroService.buscar(query);
        } else if (rol != null) {
            list = terceroService.listarPorRol(rol);
        } else {
            list = terceroService.listarTodos();
        }
        return ResponseEntity.ok(ApiResponse.ok(list, "Terceros recuperados exitosamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TerceroResponseDto>> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(terceroService.obtenerPorId(id), "Tercero encontrado"));
    }

    @GetMapping("/documento/{documento}")
    public ResponseEntity<ApiResponse<TerceroResponseDto>> obtenerPorDocumento(@PathVariable String documento) {
        return ResponseEntity.ok(ApiResponse.ok(terceroService.obtenerPorDocumento(documento), "Tercero encontrado"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TerceroResponseDto>> crear(@Valid @RequestBody TerceroRequestDto request) {
        return ResponseEntity.ok(ApiResponse.ok(terceroService.crear(request), "Tercero registrado exitosamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TerceroResponseDto>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody TerceroRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(terceroService.actualizar(id, request), "Tercero actualizado exitosamente"));
    }
}
