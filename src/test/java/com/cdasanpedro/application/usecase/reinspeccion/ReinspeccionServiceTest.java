package com.cdasanpedro.application.usecase.reinspeccion;

import com.cdasanpedro.application.dto.reinspeccion.ReinspeccionSeguimientoResponseDto;
import com.cdasanpedro.application.dto.reinspeccion.ReinspeccionVerificacionDto;
import com.cdasanpedro.core.model.enums.EstadoReinspeccion;
import com.cdasanpedro.infrastructure.persistence.entity.ClienteEntity;
import com.cdasanpedro.infrastructure.persistence.entity.OrdenIngresoEntity;
import com.cdasanpedro.infrastructure.persistence.entity.ReinspeccionSeguimientoEntity;
import com.cdasanpedro.infrastructure.persistence.entity.VehiculoEntity;
import com.cdasanpedro.infrastructure.persistence.repository.NotificacionRepository;
import com.cdasanpedro.infrastructure.persistence.repository.OrdenIngresoRepository;
import com.cdasanpedro.infrastructure.persistence.repository.ReinspeccionSeguimientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReinspeccionServiceTest {

    @Mock
    private ReinspeccionSeguimientoRepository reinspeccionRepository;

    @Mock
    private OrdenIngresoRepository ordenIngresoRepository;

    @Mock
    private NotificacionRepository notificacionRepository;

    @InjectMocks
    private ReinspeccionService service;

    private VehiculoEntity vehiculo;
    private ClienteEntity cliente;
    private OrdenIngresoEntity orden;
    private ReinspeccionSeguimientoEntity seguimiento;

    @BeforeEach
    void setUp() {
        cliente = ClienteEntity.builder()
                .id(UUID.randomUUID())
                .nombresRazonSocial("Juan Pérez")
                .celular("3101234567")
                .build();

        vehiculo = VehiculoEntity.builder()
                .id(UUID.randomUUID())
                .placa("ABC123")
                .marca("Chevrolet")
                .linea("Spark")
                .propietario(cliente)
                .build();

        orden = OrdenIngresoEntity.builder()
                .id(UUID.randomUUID())
                .consecutivo(1001L)
                .vehiculo(vehiculo)
                .conductor(cliente)
                .build();

        seguimiento = ReinspeccionSeguimientoEntity.builder()
                .id(UUID.randomUUID())
                .ordenRechazada(orden)
                .vehiculo(vehiculo)
                .cliente(cliente)
                .fechaRechazo(OffsetDateTime.now().minusDays(3))
                .fechaLimite15Dias(OffsetDateTime.now().plusDays(12))
                .estadoSeguimiento(EstadoReinspeccion.EN_PLAZO)
                .reinspeccionCompletada(false)
                .build();
    }

    @Test
    void testVerificarElegibilidadDentroDePlazo() {
        when(reinspeccionRepository.findPendientesPorPlaca("ABC123"))
                .thenReturn(Collections.singletonList(seguimiento));

        ReinspeccionVerificacionDto dto = service.verificarElegibilidad("ABC123");

        assertNotNull(dto);
        assertTrue(dto.isTieneReinspeccionGratuita());
        assertEquals("ABC123", dto.getPlaca());
        assertTrue(dto.getDiasRestantes() > 0);
    }

    @Test
    void testRegistrarRechazoGeneraAlertasEscalonadas() {
        when(ordenIngresoRepository.findById(orden.getId())).thenReturn(Optional.of(orden));
        when(reinspeccionRepository.findByOrdenRechazadaId(orden.getId())).thenReturn(Optional.empty());
        when(reinspeccionRepository.save(any(ReinspeccionSeguimientoEntity.class))).thenReturn(seguimiento);

        ReinspeccionSeguimientoResponseDto response = service.registrarRechazo(orden.getId(), "Frenos desgastados");

        assertNotNull(response);
        // Debe encolar las 5 alertas (10, 5, 3, 2 y 0 días restantes)
        verify(notificacionRepository, times(5)).save(any());
    }

    @Test
    void testRegistrarReingresoCancelaAlertas() {
        UUID ordenReinspeccionId = UUID.randomUUID();
        when(reinspeccionRepository.findByOrdenRechazadaId(orden.getId())).thenReturn(Optional.of(seguimiento));
        when(ordenIngresoRepository.findById(ordenReinspeccionId)).thenReturn(Optional.of(orden));

        service.registrarReingreso(ordenReinspeccionId, orden.getId());

        assertTrue(seguimiento.getReinspeccionCompletada());
        assertEquals(EstadoReinspeccion.REINSPECCIONADO, seguimiento.getEstadoSeguimiento());
        verify(notificacionRepository, times(1))
                .cancelarPendientesPorClienteYTipo(cliente.getId(), "RECORDATORIO_REINSPECCION");
    }
}
