package com.cdasanpedro.application.usecase.cuentapagar;

import com.cdasanpedro.application.dto.cuentapagar.CuentaPorPagarRequestDto;
import com.cdasanpedro.application.dto.cuentapagar.CuentaPorPagarResponseDto;
import com.cdasanpedro.application.dto.cuentapagar.PagoProveedorRequestDto;
import com.cdasanpedro.application.dto.cuentapagar.SemaforoVencimientosDto;
import com.cdasanpedro.core.model.enums.EstadoCuentaPagar;
import com.cdasanpedro.core.model.enums.PeriodicidadPago;
import com.cdasanpedro.core.model.enums.TipoObligacion;
import com.cdasanpedro.infrastructure.persistence.entity.CuentaPorPagarEntity;
import com.cdasanpedro.infrastructure.persistence.entity.TerceroEntity;
import com.cdasanpedro.infrastructure.persistence.entity.UsuarioEntity;
import com.cdasanpedro.infrastructure.persistence.repository.CuentaPorPagarRepository;
import com.cdasanpedro.infrastructure.persistence.repository.PagoProveedorRepository;
import com.cdasanpedro.infrastructure.persistence.repository.TerceroRepository;
import com.cdasanpedro.infrastructure.persistence.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaPorPagarServiceTest {

    @Mock
    private CuentaPorPagarRepository repository;

    @Mock
    private PagoProveedorRepository pagoRepository;

    @Mock
    private TerceroRepository terceroRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CuentaPorPagarService service;

    private TerceroEntity acreedor;
    private CuentaPorPagarEntity cuenta;

    @BeforeEach
    void setUp() {
        acreedor = TerceroEntity.builder()
                .id(UUID.randomUUID())
                .numeroDocumento("900123456")
                .razonSocialONombre("ONAC")
                .celularPrincipal("3001234567")
                .build();

        cuenta = CuentaPorPagarEntity.builder()
                .id(UUID.randomUUID())
                .acreedor(acreedor)
                .concepto("Mantenimiento Anual")
                .montoTotal(new BigDecimal("1000000.00"))
                .saldoPendiente(new BigDecimal("1000000.00"))
                .tipoObligacion(TipoObligacion.MEMBRESIA_LICENCIA)
                .periodicidad(PeriodicidadPago.MENSUAL)
                .fechaEmision(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusDays(2))
                .diasAvisoAnticipado(5)
                .estado(EstadoCuentaPagar.PENDIENTE)
                .pagos(new ArrayList<>())
                .build();
    }

    @Test
    void testCrearCuentaPorPagarExitoso() {
        CuentaPorPagarRequestDto request = CuentaPorPagarRequestDto.builder()
                .acreedorTerceroId(acreedor.getId())
                .concepto("Mantenimiento Anual")
                .montoTotal(new BigDecimal("1000000.00"))
                .tipoObligacion(TipoObligacion.MEMBRESIA_LICENCIA)
                .periodicidad(PeriodicidadPago.MENSUAL)
                .fechaVencimiento(LocalDate.now().plusDays(2))
                .build();

        when(terceroRepository.findById(acreedor.getId())).thenReturn(Optional.of(acreedor));
        when(repository.save(any(CuentaPorPagarEntity.class))).thenReturn(cuenta);

        CuentaPorPagarResponseDto response = service.crear(request);

        assertNotNull(response);
        assertEquals("Mantenimiento Anual", response.getConcepto());
        assertEquals("AMARILLO", response.getColorSemaforo());
    }

    @Test
    void testRegistrarPagoTotalGeneraSiguienteCiclo() {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .id(UUID.randomUUID())
                .nombresApellidos("Administrador")
                .build();

        PagoProveedorRequestDto request = PagoProveedorRequestDto.builder()
                .montoPagado(new BigDecimal("1000000.00"))
                .usuarioId(usuario.getId())
                .build();

        when(repository.findById(cuenta.getId())).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(repository.save(any(CuentaPorPagarEntity.class))).thenReturn(cuenta);

        CuentaPorPagarResponseDto response = service.registrarPago(cuenta.getId(), request, usuario.getId());

        assertNotNull(response);
        assertEquals(EstadoCuentaPagar.PAGADA, response.getEstado());
        assertEquals(BigDecimal.ZERO, response.getSaldoPendiente());
        // Verifica que se guardó el pago y se autogeneró la siguiente obligación periódica
        verify(repository, atLeast(2)).save(any(CuentaPorPagarEntity.class));
    }

    @Test
    void testSemaforoCalculaCorrectamente() {
        when(repository.findAllOrderByVencimientoAsc()).thenReturn(Collections.singletonList(cuenta));

        SemaforoVencimientosDto semaforo = service.obtenerSemaforoVencimientos();

        assertNotNull(semaforo);
        assertEquals(1, semaforo.getTotalProximas());
        assertEquals(0, semaforo.getTotalVencidas());
    }
}
