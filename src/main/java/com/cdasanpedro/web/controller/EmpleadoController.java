package com.cdasanpedro.web.controller;

import com.cdasanpedro.application.dto.hr.*;
import com.cdasanpedro.application.usecase.hr.EmpleadoService;
import com.cdasanpedro.core.model.enums.EstadoEmpleado;
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
@RequestMapping("/api/empleados")
@RequiredArgsConstructor
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmpleadoResponseDto>>> listarEmpleados(
            @RequestParam(required = false) EstadoEmpleado estado
    ) {
        List<EmpleadoResponseDto> empleados = empleadoService.listarTodos(estado);
        return ResponseEntity.ok(ApiResponse.ok(empleados, "Colaboradores recuperados exitosamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmpleadoResponseDto>> obtenerPorId(@PathVariable UUID id) {
        EmpleadoResponseDto empleado = empleadoService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.ok(empleado, "Colaborador encontrado"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DIRECTOR_TECNICO')")
    public ResponseEntity<ApiResponse<EmpleadoResponseDto>> crearEmpleado(
            @Valid @RequestBody EmpleadoRequestDto request
    ) {
        EmpleadoResponseDto creado = empleadoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(creado, "Colaborador '" + creado.getNombresApellidos() + "' registrado exitosamente"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DIRECTOR_TECNICO')")
    public ResponseEntity<ApiResponse<EmpleadoResponseDto>> actualizarEmpleado(
            @PathVariable UUID id,
            @Valid @RequestBody EmpleadoRequestDto request
    ) {
        EmpleadoResponseDto actualizado = empleadoService.actualizar(id, request);
        return ResponseEntity.ok(ApiResponse.ok(actualizado, "Datos del colaborador actualizados exitosamente"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> retirarEmpleado(@PathVariable UUID id) {
        empleadoService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Colaborador retirado del servicio activo"));
    }

    @PostMapping("/{id}/reenviar-invitacion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DIRECTOR_TECNICO')")
    public ResponseEntity<ApiResponse<String>> reenviarInvitacion(@PathVariable UUID id) {
        String mensaje = empleadoService.reenviarInvitacion(id);
        return ResponseEntity.ok(ApiResponse.ok(mensaje, mensaje));
    }

    // Certificaciones ONAC / ISO 17020
    @GetMapping("/certificaciones")
    public ResponseEntity<ApiResponse<List<CertificacionResponseDto>>> listarCertificaciones() {
        List<CertificacionResponseDto> certs = empleadoService.listarCertificaciones();
        return ResponseEntity.ok(ApiResponse.ok(certs, "Certificaciones técnicas recuperadas exitosamente"));
    }

    @PostMapping("/certificaciones")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DIRECTOR_TECNICO')")
    public ResponseEntity<ApiResponse<CertificacionResponseDto>> registrarCertificacion(
            @Valid @RequestBody CertificacionRequestDto request
    ) {
        CertificacionResponseDto creada = empleadoService.agregarCertificacion(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(creada, "Certificación técnica registrada exitosamente"));
    }

    @DeleteMapping("/certificaciones/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminarCertificacion(@PathVariable UUID id) {
        empleadoService.eliminarCertificacion(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Certificación eliminada exitosamente"));
    }
}
