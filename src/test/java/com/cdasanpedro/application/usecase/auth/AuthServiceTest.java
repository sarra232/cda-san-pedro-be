package com.cdasanpedro.application.usecase.auth;

import com.cdasanpedro.application.dto.auth.*;
import com.cdasanpedro.application.validator.PasswordValidator;
import com.cdasanpedro.core.exception.BusinessException;
import com.cdasanpedro.core.gateway.NotificationGateway;
import com.cdasanpedro.core.model.enums.EstadoEmpleado;
import com.cdasanpedro.core.model.enums.RolUsuario;
import com.cdasanpedro.core.model.enums.TipoDocumento;
import com.cdasanpedro.core.model.enums.TipoToken;
import com.cdasanpedro.infrastructure.persistence.entity.EmpleadoEntity;
import com.cdasanpedro.infrastructure.persistence.entity.TerceroEntity;
import com.cdasanpedro.infrastructure.persistence.entity.TokenAutenticacionEntity;
import com.cdasanpedro.infrastructure.persistence.entity.UsuarioEntity;
import com.cdasanpedro.infrastructure.persistence.repository.EmpleadoRepository;
import com.cdasanpedro.infrastructure.persistence.repository.TerceroRepository;
import com.cdasanpedro.infrastructure.persistence.repository.TokenAutenticacionRepository;
import com.cdasanpedro.infrastructure.persistence.repository.UsuarioRepository;
import com.cdasanpedro.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private TerceroRepository terceroRepository;

    @Mock
    private TokenAutenticacionRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private NotificationGateway notificationGateway;

    @Spy
    private PasswordValidator passwordValidator = new PasswordValidator();

    @InjectMocks
    private AuthService authService;

    private UsuarioEntity usuarioMock;
    private EmpleadoEntity empleadoMock;
    private TerceroEntity terceroMock;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:5173");

        terceroMock = TerceroEntity.builder()
                .id(UUID.randomUUID())
                .tipoDocumento(TipoDocumento.CC)
                .numeroDocumento("123456789")
                .razonSocialONombre("Carlos Admin")
                .emailPrincipal("carlos@cdasanpedro.com")
                .activo(true)
                .build();

        usuarioMock = UsuarioEntity.builder()
                .id(UUID.randomUUID())
                .tipoDocumento(TipoDocumento.CC)
                .numeroDocumento("123456789")
                .passwordHash("$2a$10$hashedpassword")
                .nombresApellidos("Carlos Admin")
                .rol(RolUsuario.ADMINISTRADOR)
                .activo(true)
                .build();

        empleadoMock = EmpleadoEntity.builder()
                .id(UUID.randomUUID())
                .tercero(terceroMock)
                .cargo("DIRECTOR_TECNICO")
                .estado(EstadoEmpleado.ACTIVO)
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
    @DisplayName("Debe lanzar excepción si el usuario no existe en login")
    void login_UsuarioNoExiste() {
        LoginRequestDto request = LoginRequestDto.builder()
                .numeroDocumento("999999999")
                .password("Password123*")
                .build();

        when(usuarioRepository.findByNumeroDocumento("999999999")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Debe solicitar recuperación exitosamente cuando el empleado está ACTIVO")
    void solicitarRecuperacion_EmpleadoActivo_Success() {
        SolicitudRecuperacionDto request = SolicitudRecuperacionDto.builder()
                .identificador("123456789")
                .build();

        when(terceroRepository.findByNumeroDocumento("123456789")).thenReturn(Optional.of(terceroMock));
        when(usuarioRepository.findByNumeroDocumento("123456789")).thenReturn(Optional.of(usuarioMock));
        when(empleadoRepository.findByTerceroId(terceroMock.getId())).thenReturn(Optional.of(empleadoMock));
        when(tokenRepository.save(any(TokenAutenticacionEntity.class))).thenAnswer(i -> i.getArgument(0));

        String resultado = authService.solicitarRecuperacionPassword(request);

        assertNotNull(resultado);
        verify(tokenRepository, times(1)).save(any(TokenAutenticacionEntity.class));
        verify(notificationGateway, times(1)).sendEmail(
                eq("carlos@cdasanpedro.com"),
                eq("Recuperación de Contraseña - CDA San Pedro"),
                any(),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("Debe denegar recuperación diciendo 'El correo o usuario no existe' si el empleado está RETIRADO/INACTIVO")
    void solicitarRecuperacion_EmpleadoInactivo_Error() {
        empleadoMock.setEstado(EstadoEmpleado.RETIRADO);

        SolicitudRecuperacionDto request = SolicitudRecuperacionDto.builder()
                .identificador("123456789")
                .build();

        when(terceroRepository.findByNumeroDocumento("123456789")).thenReturn(Optional.of(terceroMock));
        when(usuarioRepository.findByNumeroDocumento("123456789")).thenReturn(Optional.of(usuarioMock));
        when(empleadoRepository.findByTerceroId(terceroMock.getId())).thenReturn(Optional.of(empleadoMock));

        BusinessException ex = assertThrows(BusinessException.class, 
                () -> authService.solicitarRecuperacionPassword(request));

        assertEquals("El correo o usuario no existe", ex.getMessage());
        verify(notificationGateway, never()).sendEmail(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe denegar recuperación diciendo 'El correo o usuario no existe' si no se encuentra registro")
    void solicitarRecuperacion_NoExiste_Error() {
        SolicitudRecuperacionDto request = SolicitudRecuperacionDto.builder()
                .identificador("999999999")
                .build();

        when(terceroRepository.findByNumeroDocumento("999999999")).thenReturn(Optional.empty());
        when(usuarioRepository.findByNumeroDocumento("999999999")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, 
                () -> authService.solicitarRecuperacionPassword(request));

        assertEquals("El correo o usuario no existe", ex.getMessage());
    }

    @Test
    @DisplayName("Debe validar exitosamente un token válido de empleado activo")
    void validarToken_Success() {
        TokenAutenticacionEntity tokenMock = TokenAutenticacionEntity.builder()
                .token("tok-123")
                .usuario(usuarioMock)
                .empleado(empleadoMock)
                .tipo(TipoToken.RECUPERACION_PASSWORD)
                .emailDestinatario("carlos@cdasanpedro.com")
                .fechaExpiracion(OffsetDateTime.now().plusHours(24))
                .usado(false)
                .build();

        when(tokenRepository.findByToken("tok-123")).thenReturn(Optional.of(tokenMock));

        ValidacionTokenResponseDto response = authService.validarToken("tok-123");

        assertNotNull(response);
        assertTrue(response.isValido());
        assertEquals("RECUPERACION_PASSWORD", response.getTipo());
        assertEquals("Carlos Admin", response.getNombresApellidos());
    }

    @Test
    @DisplayName("Debe denegar validación con 'El usuario no existe' si el empleado fue retirado")
    void validarToken_EmpleadoInactivo_Error() {
        empleadoMock.setEstado(EstadoEmpleado.RETIRADO);

        TokenAutenticacionEntity tokenMock = TokenAutenticacionEntity.builder()
                .token("tok-123")
                .usuario(usuarioMock)
                .empleado(empleadoMock)
                .tipo(TipoToken.RECUPERACION_PASSWORD)
                .emailDestinatario("carlos@cdasanpedro.com")
                .fechaExpiracion(OffsetDateTime.now().plusHours(24))
                .usado(false)
                .build();

        when(tokenRepository.findByToken("tok-123")).thenReturn(Optional.of(tokenMock));

        BusinessException ex = assertThrows(BusinessException.class, 
                () -> authService.validarToken("tok-123"));

        assertEquals("El usuario no existe", ex.getMessage());
    }

    @Test
    @DisplayName("Debe establecer contraseña exitosamente con token válido y políticas de clave")
    void establecerPassword_Success() {
        TokenAutenticacionEntity tokenMock = TokenAutenticacionEntity.builder()
                .token("tok-123")
                .usuario(usuarioMock)
                .empleado(empleadoMock)
                .tipo(TipoToken.INVITACION)
                .emailDestinatario("carlos@cdasanpedro.com")
                .fechaExpiracion(OffsetDateTime.now().plusHours(48))
                .usado(false)
                .build();

        RestablecerPasswordDto request = RestablecerPasswordDto.builder()
                .token("tok-123")
                .newPassword("NuevaPass2026*")
                .confirmPassword("NuevaPass2026*")
                .build();

        when(tokenRepository.findByToken("tok-123")).thenReturn(Optional.of(tokenMock));
        when(passwordEncoder.encode("NuevaPass2026*")).thenReturn("$2a$10$newhashedpassword");

        String mensaje = authService.establecerPassword(request);

        assertNotNull(mensaje);
        assertTrue(tokenMock.getUsado());
        verify(usuarioRepository, times(1)).save(usuarioMock);
        verify(tokenRepository, times(1)).save(tokenMock);
    }
}
