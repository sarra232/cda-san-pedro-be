package com.cdasanpedro.application.usecase.auth;

import com.cdasanpedro.application.dto.auth.LoginRequestDto;
import com.cdasanpedro.application.dto.auth.LoginResponseDto;
import com.cdasanpedro.core.exception.BusinessException;
import com.cdasanpedro.core.model.enums.RolUsuario;
import com.cdasanpedro.core.model.enums.TipoDocumento;
import com.cdasanpedro.infrastructure.persistence.entity.UsuarioEntity;
import com.cdasanpedro.infrastructure.persistence.repository.UsuarioRepository;
import com.cdasanpedro.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private UsuarioEntity usuarioMock;

    @BeforeEach
    void setUp() {
        usuarioMock = UsuarioEntity.builder()
                .id(UUID.randomUUID())
                .tipoDocumento(TipoDocumento.CC)
                .numeroDocumento("123456789")
                .passwordHash("$2a$10$hashedpassword")
                .nombresApellidos("Carlos Admin")
                .rol(RolUsuario.ADMINISTRADOR)
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("Debe autenticar exitosamente con credenciales válidas y retornar JWT")
    void login_Success() {
        LoginRequestDto request = LoginRequestDto.builder()
                .numeroDocumento("123456789")
                .password("Password123*")
                .build();

        when(usuarioRepository.findByNumeroDocumento("123456789")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("Password123*", "$2a$10$hashedpassword")).thenReturn(true);
        when(jwtService.generateToken(any(UsuarioEntity.class))).thenReturn("jwt-token-valido-xyz");

        LoginResponseDto response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token-valido-xyz", response.getToken());
        assertEquals("123456789", response.getNumeroDocumento());
        assertEquals(RolUsuario.ADMINISTRADOR, response.getRol());
        verify(usuarioRepository, times(1)).save(usuarioMock);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el usuario no existe")
    void login_UsuarioNoExiste() {
        LoginRequestDto request = LoginRequestDto.builder()
                .numeroDocumento("999999999")
                .password("Password123*")
                .build();

        when(usuarioRepository.findByNumeroDocumento("999999999")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Debe lanzar excepción si la contraseña es incorrecta")
    void login_PasswordIncorrecto() {
        LoginRequestDto request = LoginRequestDto.builder()
                .numeroDocumento("123456789")
                .password("PasswordInvalido")
                .build();

        when(usuarioRepository.findByNumeroDocumento("123456789")).thenReturn(Optional.of(usuarioMock));
        when(passwordEncoder.matches("PasswordInvalido", "$2a$10$hashedpassword")).thenReturn(false);

        assertThrows(BusinessException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el usuario está inactivo")
    void login_UsuarioInactivo() {
        usuarioMock.setActivo(false);
        LoginRequestDto request = LoginRequestDto.builder()
                .numeroDocumento("123456789")
                .password("Password123*")
                .build();

        when(usuarioRepository.findByNumeroDocumento("123456789")).thenReturn(Optional.of(usuarioMock));

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(request));
        assertTrue(exception.getMessage().contains("inactivo"));
    }
}
