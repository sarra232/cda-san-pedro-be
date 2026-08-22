package com.cdasanpedro.web.controller;

import com.cdasanpedro.application.dto.auth.LoginRequestDto;
import com.cdasanpedro.application.dto.auth.LoginResponseDto;
import com.cdasanpedro.application.dto.auth.UsuarioPerfilDto;
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
}
