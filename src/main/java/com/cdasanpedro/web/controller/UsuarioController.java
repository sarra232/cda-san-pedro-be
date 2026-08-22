package com.cdasanpedro.web.controller;

import com.cdasanpedro.application.dto.usuario.CreateUsuarioDto;
import com.cdasanpedro.application.dto.usuario.UpdateUsuarioDto;
import com.cdasanpedro.application.dto.usuario.UsuarioDto;
import com.cdasanpedro.application.usecase.usuario.UsuarioService;
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
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UsuarioDto>>> listarUsuarios() {
        List<UsuarioDto> usuarios = usuarioService.listarTodos();
        return ResponseEntity.ok(ApiResponse.ok(usuarios, "Usuarios recuperados"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioDto>> obtenerPorId(@PathVariable UUID id) {
        UsuarioDto usuario = usuarioService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.ok(usuario, "Usuario encontrado"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<UsuarioDto>> crearUsuario(@Valid @RequestBody CreateUsuarioDto request) {
        UsuarioDto usuario = usuarioService.crearUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(usuario, "Usuario creado exitosamente"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<UsuarioDto>> actualizarUsuario(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUsuarioDto request
    ) {
        UsuarioDto usuario = usuarioService.actualizarUsuario(id, request);
        return ResponseEntity.ok(ApiResponse.ok(usuario, "Usuario actualizado exitosamente"));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<UsuarioDto>> cambiarEstado(
            @PathVariable UUID id,
            @RequestParam boolean activo
    ) {
        UsuarioDto usuario = usuarioService.cambiarEstado(id, activo);
        return ResponseEntity.ok(ApiResponse.ok(usuario, "Estado de usuario actualizado exitosamente"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> eliminarUsuario(@PathVariable UUID id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Usuario eliminado exitosamente"));
    }
}
