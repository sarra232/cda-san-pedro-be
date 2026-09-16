package com.cdasanpedro.application.usecase.reporte;

import com.cdasanpedro.application.dto.reporte.DashboardStatsDto;
import com.cdasanpedro.application.dto.reporte.ReporteVentasDto;
import com.cdasanpedro.application.usecase.factura.FacturaService;
import com.cdasanpedro.application.usecase.vehiculo.VehiculoService;
import com.cdasanpedro.core.model.enums.CategoriaVehiculo;
import com.cdasanpedro.core.model.enums.EstadoFactura;
import com.cdasanpedro.core.model.enums.MetodoPago;
import com.cdasanpedro.infrastructure.persistence.entity.FacturaEntity;
import com.cdasanpedro.infrastructure.persistence.entity.OrdenIngresoEntity;
import com.cdasanpedro.infrastructure.persistence.entity.VehiculoEntity;
import com.cdasanpedro.infrastructure.persistence.repository.FacturaRepository;
import com.cdasanpedro.infrastructure.persistence.repository.OrdenIngresoRepository;
import com.cdasanpedro.infrastructure.persistence.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private OrdenIngresoRepository ordenIngresoRepository;
    @Mock
    private VehiculoRepository vehiculoRepository;
    @Mock
    private FacturaService facturaService;
    @Mock
    private VehiculoService vehiculoService;

    @InjectMocks
    private ReporteService reporteService;

    private FacturaEntity facturaMock;
    private OrdenIngresoEntity ordenMock;
    private VehiculoEntity vehiculoMock;

    @BeforeEach
    void setUp() {
        vehiculoMock = VehiculoEntity.builder()
                .id(UUID.randomUUID())
                .placa("ABC123")
                .categoria(CategoriaVehiculo.LIVIANO)
                .marca("CHEVROLET")
                .linea("SAIL")
                .modelo(2021)
                .build();

        ordenMock = OrdenIngresoEntity.builder()
                .id(UUID.randomUUID())
                .consecutivo(1L)
                .fechaIngreso(OffsetDateTime.now())
                .vehiculo(vehiculoMock)
                .build();

        facturaMock = FacturaEntity.builder()
                .id(UUID.randomUUID())
                .numeroFactura("FAC-00001")
                .fechaEmision(OffsetDateTime.now())
                .total(new BigDecimal("320000.00"))
                .subtotal(new BigDecimal("268907.56"))
                .iva(new BigDecimal("51092.44"))
                .metodoPago(MetodoPago.EFECTIVO)
                .estado(EstadoFactura.PAGADA)
                .ordenIngreso(ordenMock)
                .build();
    }

    @Test
    @DisplayName("Debe calcular correctamente las métricas en tiempo real del Dashboard")
    void obtenerDashboardStats() {
        when(ordenIngresoRepository.findByFechaIngresoBetween(any(), any())).thenReturn(List.of(ordenMock));
        when(facturaRepository.findByFechaEmisionBetween(any(), any())).thenReturn(List.of(facturaMock));
        when(vehiculoRepository.findAll()).thenReturn(List.of(vehiculoMock));
        when(vehiculoService.toDto(vehiculoMock)).thenReturn(
                com.cdasanpedro.application.dto.vehiculo.VehiculoResponseDto.builder()
                        .placa("ABC123")
                        .categoria(CategoriaVehiculo.LIVIANO)
                        .soatVencido(false)
                        .rtmVencido(false)
                        .soatProximoVencer(false)
                        .rtmProximoVencer(false)
                        .build()
        );

        DashboardStatsDto stats = reporteService.obtenerDashboardStats();

        assertNotNull(stats);
        assertEquals(new BigDecimal("320000.00"), stats.getRecaudoHoy());
        assertEquals(1L, stats.getVehiculosAtendidosHoy());
        assertEquals(1L, stats.getFacturasEmitidasHoy());
        assertEquals(1L, stats.getTotalLivianos());
        assertEquals(0L, stats.getTotalMotos());
        assertEquals(10, stats.getActividadPorHoras().size());
    }

    @Test
    @DisplayName("Debe generar reporte de ventas por rango de fechas")
    void generarReporteVentas() {
        when(facturaRepository.findByFechaEmisionBetween(any(), any())).thenReturn(List.of(facturaMock));

        ReporteVentasDto reporte = reporteService.generarReporteVentas(LocalDate.now().minusDays(7), LocalDate.now());

        assertNotNull(reporte);
        assertEquals(new BigDecimal("320000.00"), reporte.getTotalRecaudado());
        assertEquals(1L, reporte.getTotalFacturas());
        assertTrue(reporte.getVehiculosPorCategoria().containsKey("LIVIANO"));
        assertTrue(reporte.getIngresosPorMetodoPago().containsKey("EFECTIVO"));
    }
}
