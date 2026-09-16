package com.cdasanpedro.application.usecase.hr;

import com.cdasanpedro.application.dto.hr.EmpleadoRequestDto;
import com.cdasanpedro.application.dto.hr.EmpleadoResponseDto;
import com.cdasanpedro.core.gateway.NotificationGateway;
import com.cdasanpedro.core.model.enums.EstadoEmpleado;
import com.cdasanpedro.core.model.enums.TipoContrato;
import com.cdasanpedro.core.model.enums.TipoDocumento;
import com.cdasanpedro.infrastructure.persistence.entity.EmpleadoEntity;
import com.cdasanpedro.infrastructure.persistence.entity.TerceroEntity;
import com.cdasanpedro.infrastructure.persistence.entity.TokenAutenticacionEntity;
import com.cdasanpedro.infrastructure.persistence.entity.UsuarioEntity;
import com.cdasanpedro.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpleadoServiceTest {

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private EmpleadoCertificacionRepository certificacionRepository;

    @Mock
    private TerceroRepository terceroRepository;

    @Mock
    private TerceroRolRepository terceroRolRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TokenAutenticacionRepository tokenRepository;

    @Mock
    private NotificationGateway notificationGateway;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmpleadoService empleadoService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(empleadoService, "frontendUrl", "http://localhost:5173");
    }

    @Test
    @DisplayName("Debe crear empleado, generar token de invitación y despachar correo de activación")
    void crearEmpleado_EnviaInvitacion_Success() {
        EmpleadoRequestDto request = EmpleadoRequestDto.builder()
                .tipoDocumento("CC")
                .numeroDocumento("1020304050")
                .nombresApellidos("Andrés Gómez")
                .celular("3128899001")
                .email("andres.gomez@cdasanpedro.com")
                .cargo("INSPECTOR_LINEA_LIVIANOS")
                .departamento("OPERACIONES_PISTA")
                .tipoContrato(TipoContrato.TERMINO_INDEFINIDO)
                .salarioBase(new BigDecimal("1850000.00"))
                .auxilioTransporteAplica(true)
                .fechaIngreso(LocalDate.now())
                .estado(EstadoEmpleado.ACTIVO)
                .build();

        TerceroEntity terceroSaved = TerceroEntity.builder()
                .id(UUID.randomUUID())
                .tipoDocumento(TipoDocumento.CC)
                .numeroDocumento("1020304050")
                .razonSocialONombre("Andrés Gómez")
                .emailPrincipal("andres.gomez@cdasanpedro.com")
                .activo(true)
                .build();

        EmpleadoEntity empleadoSaved = EmpleadoEntity.builder()
                .id(UUID.randomUUID())
                .tercero(terceroSaved)
                .cargo("INSPECTOR_LINEA_LIVIANOS")
                .departamento("OPERACIONES_PISTA")
                .tipoContrato(TipoContrato.TERMINO_INDEFINIDO)
                .salarioBase(new BigDecimal("1850000.00"))
                .auxilioTransporteAplica(true)
                .fechaIngreso(LocalDate.now())
                .estado(EstadoEmpleado.ACTIVO)
                .certificaciones(new ArrayList<>())
                .build();

        when(terceroRepository.findByNumeroDocumento("1020304050")).thenReturn(Optional.empty());
        when(terceroRepository.save(any(TerceroEntity.class))).thenReturn(terceroSaved);
        when(empleadoRepository.save(any(EmpleadoEntity.class))).thenReturn(empleadoSaved);
        when(usuarioRepository.findByNumeroDocumento("1020304050")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(any())).thenReturn("hashed-initial-pass");
        when(tokenRepository.save(any(TokenAutenticacionEntity.class))).thenAnswer(i -> i.getArgument(0));

        EmpleadoResponseDto response = empleadoService.crear(request);

        assertNotNull(response);
        assertEquals("1020304050", response.getNumeroDocumento());
        assertEquals("INSPECTOR_LINEA_LIVIANOS", response.getCargo());

        verify(tokenRepository, times(1)).save(any(TokenAutenticacionEntity.class));
        verify(notificationGateway, times(1)).sendEmail(
                eq("andres.gomez@cdasanpedro.com"),
                eq("Invitación y Activación de Cuenta - CDA San Pedro"),
                any(),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("Debe actualizar el rol de usuario y estado en cascada al modificar empleado")
    void actualizarEmpleado_SincronizaRolYUsuario_Success() {
        UUID empleadoId = UUID.randomUUID();
        TerceroEntity tercero = TerceroEntity.builder()
                .id(UUID.randomUUID())
                .numeroDocumento("1020304050")
                .razonSocialONombre("Carlos Técnico")
                .activo(true)
                .build();

        EmpleadoEntity empleado = EmpleadoEntity.builder()
                .id(empleadoId)
                .tercero(tercero)
                .cargo("INSPECTOR_LINEA_LIVIANOS")
                .estado(EstadoEmpleado.ACTIVO)
                .salarioBase(new BigDecimal("2000000.00"))
                .certificaciones(new ArrayList<>())
                .build();

        UsuarioEntity usuario = UsuarioEntity.builder()
                .id(UUID.randomUUID())
                .numeroDocumento("1020304050")
                .rol(com.cdasanpedro.core.model.enums.RolUsuario.TECNICO_PISTA)
                .activo(true)
                .build();

        EmpleadoRequestDto updateReq = EmpleadoRequestDto.builder()
                .cargo("DIRECTOR_TECNICO")
                .rolApp(com.cdasanpedro.core.model.enums.RolUsuario.DIRECTOR_TECNICO)
                .estado(EstadoEmpleado.ACTIVO)
                .build();

        when(empleadoRepository.findById(empleadoId)).thenReturn(Optional.of(empleado));
        when(usuarioRepository.findByNumeroDocumento("1020304050")).thenReturn(Optional.of(usuario));
        when(empleadoRepository.save(any(EmpleadoEntity.class))).thenAnswer(i -> i.getArgument(0));

        EmpleadoResponseDto response = empleadoService.actualizar(empleadoId, updateReq);

        assertNotNull(response);
        assertEquals(com.cdasanpedro.core.model.enums.RolUsuario.DIRECTOR_TECNICO, usuario.getRol());
        assertTrue(usuario.getActivo());
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    @DisplayName("Debe inhabilitar la cuenta de usuario en cascada al retirar al empleado")
    void eliminarEmpleado_InhabilitaUsuarioEnCascada_Success() {
        UUID empleadoId = UUID.randomUUID();
        TerceroEntity tercero = TerceroEntity.builder()
                .id(UUID.randomUUID())
                .numeroDocumento("1020304050")
                .razonSocialONombre("Carlos Retirado")
                .activo(true)
                .build();

        EmpleadoEntity empleado = EmpleadoEntity.builder()
                .id(empleadoId)
                .tercero(tercero)
                .cargo("TECNICO_PISTA")
                .estado(EstadoEmpleado.ACTIVO)
                .build();

        UsuarioEntity usuario = UsuarioEntity.builder()
                .id(UUID.randomUUID())
                .numeroDocumento("1020304050")
                .activo(true)
                .build();

        when(empleadoRepository.findById(empleadoId)).thenReturn(Optional.of(empleado));
        when(usuarioRepository.findByNumeroDocumento("1020304050")).thenReturn(Optional.of(usuario));

        empleadoService.eliminar(empleadoId);

        assertEquals(EstadoEmpleado.RETIRADO, empleado.getEstado());
        assertNotNull(empleado.getFechaRetiro());
        assertFalse(usuario.getActivo());
        verify(usuarioRepository, times(1)).save(usuario);
    }
}
