package com.cdasanpedro.application.usecase.ingreso;

import com.cdasanpedro.application.dto.cliente.ClienteResponseDto;
import com.cdasanpedro.application.dto.ingreso.OrdenIngresoRequestDto;
import com.cdasanpedro.application.dto.ingreso.OrdenIngresoResponseDto;
import com.cdasanpedro.application.dto.vehiculo.VehiculoResponseDto;
import com.cdasanpedro.application.usecase.cliente.ClienteService;
import com.cdasanpedro.application.usecase.vehiculo.VehiculoService;
import com.cdasanpedro.core.model.enums.CategoriaVehiculo;
import com.cdasanpedro.core.model.enums.EstadoOrden;
import com.cdasanpedro.core.model.enums.RolUsuario;
import com.cdasanpedro.core.model.enums.TipoDocumento;
import com.cdasanpedro.infrastructure.persistence.entity.ClienteEntity;
import com.cdasanpedro.infrastructure.persistence.entity.OrdenIngresoEntity;
import com.cdasanpedro.infrastructure.persistence.entity.UsuarioEntity;
import com.cdasanpedro.infrastructure.persistence.entity.VehiculoEntity;
import com.cdasanpedro.infrastructure.persistence.repository.ClienteRepository;
import com.cdasanpedro.infrastructure.persistence.repository.OrdenIngresoRepository;
import com.cdasanpedro.infrastructure.persistence.repository.PruebaInspeccionRepository;
import com.cdasanpedro.infrastructure.persistence.repository.UsuarioRepository;
import com.cdasanpedro.infrastructure.persistence.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenIngresoServiceTest {

    @Mock
    private OrdenIngresoRepository ordenIngresoRepository;
    @Mock
    private VehiculoRepository vehiculoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ClienteService clienteService;
    @Mock
    private VehiculoService vehiculoService;
    @Mock
    private PruebaInspeccionRepository pruebaInspeccionRepository;
    @Mock
    private com.cdasanpedro.application.usecase.reinspeccion.ReinspeccionService reinspeccionService;
    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private OrdenIngresoService ordenIngresoService;

    private UsuarioEntity usuarioMock;
    private ClienteEntity propietarioMock;
    private ClienteEntity conductorTerceroMock;
    private VehiculoEntity vehiculoMock;

    @BeforeEach
    void setUp() {
        usuarioMock = UsuarioEntity.builder()
                .id(UUID.randomUUID())
                .nombresApellidos("Recepcionista Turno")
                .rol(RolUsuario.RECEPCIONISTA)
                .build();

        propietarioMock = ClienteEntity.builder()
                .id(UUID.randomUUID())
                .tipoDocumento(TipoDocumento.CC)
                .numeroDocumento("1020304050")
                .nombresRazonSocial("Mauricio Propietario")
                .celular("3001234567")
                .build();

        conductorTerceroMock = ClienteEntity.builder()
                .id(UUID.randomUUID())
                .tipoDocumento(TipoDocumento.CC)
                .numeroDocumento("9080706050")
                .nombresRazonSocial("Pedro Conductor Tercero")
                .celular("3159998877")
                .build();

        vehiculoMock = VehiculoEntity.builder()
                .id(UUID.randomUUID())
                .placa("ABC123")
                .categoria(CategoriaVehiculo.LIVIANO)
                .marca("CHEVROREST")
                .linea("SAIL")
                .modelo(2021)
                .propietario(propietarioMock)
                .build();

        lenient().when(clienteService.toDto(any(ClienteEntity.class))).thenAnswer(inv -> {
            ClienteEntity c = inv.getArgument(0);
            return ClienteResponseDto.builder()
                    .id(c.getId())
                    .numeroDocumento(c.getNumeroDocumento())
                    .nombresRazonSocial(c.getNombresRazonSocial())
                    .celular(c.getCelular())
                    .build();
        });

        lenient().when(vehiculoService.toDto(any(VehiculoEntity.class))).thenAnswer(inv -> {
            VehiculoEntity v = inv.getArgument(0);
            return VehiculoResponseDto.builder()
                    .id(v.getId())
                    .placa(v.getPlaca())
                    .categoria(v.getCategoria())
                    .marca(v.getMarca())
                    .linea(v.getLinea())
                    .modelo(v.getModelo())
                    .propietario(ClienteResponseDto.builder()
                            .id(v.getPropietario().getId())
                            .numeroDocumento(v.getPropietario().getNumeroDocumento())
                            .nombresRazonSocial(v.getPropietario().getNombresRazonSocial())
                            .celular(v.getPropietario().getCelular())
                            .build())
                    .build();
        });
    }

    @Test
    @DisplayName("Debe registrar ingreso cuando el conductor es el mismo propietario")
    void registrarIngreso_ConductorEsPropietario() {
        OrdenIngresoRequestDto request = OrdenIngresoRequestDto.builder()
                .placa("ABC123")
                .kilometraje(45000)
                .tipoServicio("RTM_LEGAL")
                .conductorEsPropietario(true)
                .observaciones("Vehículo en buen estado")
                .build();

        OrdenIngresoEntity ordenGuardada = OrdenIngresoEntity.builder()
                .id(UUID.randomUUID())
                .consecutivo(1001L)
                .fechaIngreso(OffsetDateTime.now())
                .kilometraje(45000)
                .tipoServicio("RTM_LEGAL")
                .estado(EstadoOrden.INGRESADO)
                .conductorEsPropietario(true)
                .conductor(propietarioMock)
                .vehiculo(vehiculoMock)
                .usuario(usuarioMock)
                .esReinspeccion(false)
                .build();

        when(usuarioRepository.findById(usuarioMock.getId())).thenReturn(Optional.of(usuarioMock));
        when(vehiculoRepository.findByPlaca("ABC123")).thenReturn(Optional.of(vehiculoMock));
        when(ordenIngresoRepository.saveAndFlush(any(OrdenIngresoEntity.class))).thenReturn(ordenGuardada);
        lenient().when(clienteService.toDto(any(ClienteEntity.class))).thenAnswer(inv -> {
            ClienteEntity c = inv.getArgument(0);
            return ClienteResponseDto.builder()
                    .id(c.getId())
                    .numeroDocumento(c.getNumeroDocumento())
                    .nombresRazonSocial(c.getNombresRazonSocial())
                    .celular(c.getCelular())
                    .build();
        });

        OrdenIngresoResponseDto response = ordenIngresoService.registrarIngreso(request, usuarioMock.getId());

        assertNotNull(response);
        assertEquals(1001L, response.getConsecutivo());
        assertTrue(response.getConductorEsPropietario());
        assertEquals("ABC123", response.getVehiculo().getPlaca());
        verify(ordenIngresoRepository, times(1)).saveAndFlush(any(OrdenIngresoEntity.class));
    }

    @Test
    @DisplayName("Debe registrar ingreso cuando el conductor es un tercero distinto al propietario")
    void registrarIngreso_ConductorEsTercero() {
        OrdenIngresoRequestDto request = OrdenIngresoRequestDto.builder()
                .placa("ABC123")
                .kilometraje(45000)
                .tipoServicio("RTM_LEGAL")
                .conductorEsPropietario(false)
                .conductorId(conductorTerceroMock.getId())
                .observaciones("Ingresado por conductor contratado")
                .build();

        OrdenIngresoEntity ordenGuardada = OrdenIngresoEntity.builder()
                .id(UUID.randomUUID())
                .consecutivo(1002L)
                .fechaIngreso(OffsetDateTime.now())
                .kilometraje(45000)
                .tipoServicio("RTM_LEGAL")
                .estado(EstadoOrden.INGRESADO)
                .conductorEsPropietario(false)
                .conductor(conductorTerceroMock)
                .vehiculo(vehiculoMock)
                .usuario(usuarioMock)
                .esReinspeccion(false)
                .build();

        when(usuarioRepository.findById(usuarioMock.getId())).thenReturn(Optional.of(usuarioMock));
        when(vehiculoRepository.findByPlaca("ABC123")).thenReturn(Optional.of(vehiculoMock));
        when(clienteRepository.findById(conductorTerceroMock.getId())).thenReturn(Optional.of(conductorTerceroMock));
        when(ordenIngresoRepository.saveAndFlush(any(OrdenIngresoEntity.class))).thenReturn(ordenGuardada);
        lenient().when(clienteService.toDto(any(ClienteEntity.class))).thenAnswer(inv -> {
            ClienteEntity c = inv.getArgument(0);
            return ClienteResponseDto.builder()
                    .id(c.getId())
                    .numeroDocumento(c.getNumeroDocumento())
                    .nombresRazonSocial(c.getNombresRazonSocial())
                    .celular(c.getCelular())
                    .build();
        });

        OrdenIngresoResponseDto response = ordenIngresoService.registrarIngreso(request, usuarioMock.getId());

        assertNotNull(response);
        assertEquals(1002L, response.getConsecutivo());
        assertFalse(response.getConductorEsPropietario());
        assertEquals("Pedro Conductor Tercero", response.getConductor().getNombresRazonSocial());
        assertEquals("Mauricio Propietario", response.getVehiculo().getPropietario().getNombresRazonSocial());
    }
}
