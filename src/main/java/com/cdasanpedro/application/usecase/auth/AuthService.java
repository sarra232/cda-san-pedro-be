package com.cdasanpedro.application.usecase.auth;

import com.cdasanpedro.application.dto.auth.LoginRequestDto;
import com.cdasanpedro.application.dto.auth.LoginResponseDto;
import com.cdasanpedro.application.dto.auth.UsuarioPerfilDto;
import com.cdasanpedro.core.exception.BusinessException;
import com.cdasanpedro.core.exception.ResourceNotFoundException;
import com.cdasanpedro.infrastructure.persistence.entity.UsuarioEntity;
import com.cdasanpedro.infrastructure.persistence.repository.UsuarioRepository;
import com.cdasanpedro.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        String numeroDoc = request.getNumeroDocumento().trim();

        UsuarioEntity usuario = usuarioRepository.findByNumeroDocumento(numeroDoc)
                .orElseThrow(() -> new BusinessException("Credenciales incorrectas: Número de documento o contraseña inválidos"));

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new BusinessException("El usuario se encuentra inactivo en el sistema. Contacte al administrador.");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new BusinessException("Credenciales incorrectas: Número de documento o contraseña inválidos");
        }

        // Actualizar fecha de último login
        usuario.setUltimoLogin(OffsetDateTime.now());
        usuarioRepository.save(usuario);

        String token = jwtService.generateToken(usuario);

        return LoginResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .id(usuario.getId())
                .tipoDocumento(usuario.getTipoDocumento())
                .numeroDocumento(usuario.getNumeroDocumento())
                .nombresApellidos(usuario.getNombresApellidos())
                .rol(usuario.getRol())
                .build();
    }

    @Transactional(readOnly = true)
    public UsuarioPerfilDto getPerfil(UUID usuarioId) {
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return UsuarioPerfilDto.builder()
                .id(usuario.getId())
                .tipoDocumento(usuario.getTipoDocumento())
                .numeroDocumento(usuario.getNumeroDocumento())
                .nombresApellidos(usuario.getNombresApellidos())
                .rol(usuario.getRol())
                .activo(usuario.getActivo())
                .ultimoLogin(usuario.getUltimoLogin())
                .build();
    }
}
