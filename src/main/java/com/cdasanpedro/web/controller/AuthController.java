package com.cdasanpedro.web.controller;

import com.cdasanpedro.application.dto.auth.*;
import com.cdasanpedro.application.usecase.auth.AuthService;
import com.cdasanpedro.web.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Inicio de sesión exitoso"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UsuarioPerfilDto>> getMiPerfil(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("No autenticado"));
        }
        UUID userId = (UUID) authentication.getCredentials();
        UsuarioPerfilDto perfil = authService.getPerfil(userId);
        return ResponseEntity.ok(ApiResponse.ok(perfil, "Perfil de usuario cargado"));
    }

    @PostMapping({"/solicitar-recuperacion", "/recuperar-password", "/forgot-password"})
    public ResponseEntity<ApiResponse<String>> solicitarRecuperacion(@Valid @RequestBody SolicitudRecuperacionDto request) {
        String mensaje = authService.solicitarRecuperacionPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(mensaje, mensaje));
    }

    @GetMapping({"/validar-token", "/validate-token"})
    public ResponseEntity<ApiResponse<ValidacionTokenResponseDto>> validarToken(@RequestParam("token") String token) {
        ValidacionTokenResponseDto response = authService.validarToken(token);
        return ResponseEntity.ok(ApiResponse.ok(response, "Token de seguridad válido"));
    }

    @PostMapping({"/establecer-password", "/restablecer-password", "/reset-password"})
    public ResponseEntity<ApiResponse<String>> establecerPassword(@Valid @RequestBody RestablecerPasswordDto request) {
        String mensaje = authService.establecerPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(mensaje, mensaje));
    }
}
