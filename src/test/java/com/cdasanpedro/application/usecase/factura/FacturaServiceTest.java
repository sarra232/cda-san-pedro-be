package com.cdasanpedro.application.usecase.factura;

import com.cdasanpedro.application.dto.cliente.ClienteResponseDto;
import com.cdasanpedro.application.dto.factura.FacturaRequestDto;
import com.cdasanpedro.application.dto.factura.FacturaResponseDto;
import com.cdasanpedro.application.dto.ingreso.OrdenIngresoResponseDto;
import com.cdasanpedro.application.dto.vehiculo.VehiculoResponseDto;
import com.cdasanpedro.application.usecase.cliente.ClienteService;
import com.cdasanpedro.application.usecase.ingreso.OrdenIngresoService;
import com.cdasanpedro.core.model.enums.*;
import com.cdasanpedro.infrastructure.pdf.PdfGeneratorService;
import com.cdasanpedro.infrastructure.persistence.entity.*;
import com.cdasanpedro.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacturaServiceTest {

    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private OrdenIngresoRepository ordenIngresoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ClienteService clienteService;
    @Mock
    private OrdenIngresoService ordenIngresoService;
    @Mock
    private PdfGeneratorService pdfGeneratorService;
    @Mock
    private com.cdasanpedro.application.usecase.notificacion.NotificacionService notificacionService;
    @Mock
    private com.cdasanpedro.application.usecase.tarifa.TarifaService tarifaService;

    @InjectMocks
    private FacturaService facturaService;

    private UsuarioEntity cajeroMock;
    private ClienteEntity propietarioMock;
    private VehiculoEntity vehiculoMock;
    private OrdenIngresoEntity ordenMock;

    @BeforeEach
    void setUp() {
        cajeroMock = UsuarioEntity.builder()
                .id(UUID.randomUUID())
                .nombresApellidos("Cajero Central")
                .rol(RolUsuario.RECEPCIONISTA)
                .build();

        propietarioMock = ClienteEntity.builder()
                .id(UUID.randomUUID())
                .tipoDocumento(TipoDocumento.CC)
                .numeroDocumento("1020304050")
                .nombresRazonSocial("Mauricio Propietario")
                .celular("3001234567")
                .build();

        vehiculoMock = VehiculoEntity.builder()
                .id(UUID.randomUUID())
                .placa("ABC123")
                .categoria(CategoriaVehiculo.LIVIANO)
                .marca("CHEVROLET")
                .linea("SAIL")
                .modelo(2021)
                .propietario(propietarioMock)
                .build();

        ordenMock = OrdenIngresoEntity.builder()
                .id(UUID.randomUUID())
                .consecutivo(1L)
                .fechaIngreso(OffsetDateTime.now())
                .kilometraje(45000)
                .tipoServicio("RTM_LEGAL")
                .estado(EstadoOrden.APROBADO)
                .conductorEsPropietario(true)
                .conductor(propietarioMock)
                .vehiculo(vehiculoMock)
                .usuario(cajeroMock)
                .build();
    }

    @Test
    @DisplayName("Debe emitir factura con cálculo de IVA 19% y numeración consecutiva")
    void emitirFactura_CalculoIvaYConsecutivo() {
        FacturaRequestDto request = FacturaRequestDto.builder()
                .ordenIngresoId(ordenMock.getId())
                .pagadorTipo("PROPIETARIO")
                .metodoPago(MetodoPago.EFECTIVO)
                .build();

        FacturaEntity facturaGuardada = FacturaEntity.builder()
                .id(UUID.randomUUID())
                .numeroFactura("FAC-00001")
                .fechaEmision(OffsetDateTime.now())
                .subtotal(new BigDecimal("268907.56"))
                .iva(new BigDecimal("51092.44"))
                .total(new BigDecimal("320000.00"))
                .metodoPago(MetodoPago.EFECTIVO)
                .estado(EstadoFactura.PAGADA)
                .ordenIngreso(ordenMock)
                .clienteFactura(propietarioMock)
                .usuario(cajeroMock)
                .build();

        when(ordenIngresoRepository.findById(ordenMock.getId())).thenReturn(Optional.of(ordenMock));
        when(facturaRepository.findByOrdenIngresoId(ordenMock.getId())).thenReturn(Optional.empty());
        when(usuarioRepository.findById(cajeroMock.getId())).thenReturn(Optional.of(cajeroMock));
        when(facturaRepository.count()).thenReturn(0L);
        when(tarifaService.obtenerPrecioPorCategoria(any())).thenReturn(new BigDecimal("320000.00"));
        when(facturaRepository.save(any(FacturaEntity.class))).thenReturn(facturaGuardada);

        when(clienteService.toDto(propietarioMock)).thenReturn(
                ClienteResponseDto.builder().numeroDocumento("1020304050").nombresRazonSocial("Mauricio Propietario").build()
        );
        when(ordenIngresoService.toDto(ordenMock)).thenReturn(
                OrdenIngresoResponseDto.builder()
                        .consecutivo(1L)
                        .vehiculo(VehiculoResponseDto.builder().placa("ABC123").categoria(CategoriaVehiculo.LIVIANO).build())
                        .build()
        );

        FacturaResponseDto response = facturaService.emitirFactura(request, cajeroMock.getId());

        assertNotNull(response);
        assertEquals("FAC-00001", response.getNumeroFactura());
        assertEquals(new BigDecimal("320000.00"), response.getTotal());
        assertEquals(MetodoPago.EFECTIVO, response.getMetodoPago());
        assertEquals(EstadoOrden.FACTURADO, ordenMock.getEstado());
        verify(facturaRepository, times(1)).save(any(FacturaEntity.class));
    }

    @Test
    @DisplayName("Debe emitir factura a $0 para reinspección gratuita de 15 días")
    void emitirFactura_ReinspeccionGratuita_TotalCero() {
        ordenMock.setEsReinspeccion(true);
        ordenMock.setTipoServicio("REINSPECCION_GRATUITA");

        FacturaRequestDto request = FacturaRequestDto.builder()
                .ordenIngresoId(ordenMock.getId())
                .pagadorTipo("PROPIETARIO")
                .metodoPago(MetodoPago.EFECTIVO)
                .build();

        FacturaEntity facturaGuardada = FacturaEntity.builder()
                .id(UUID.randomUUID())
                .numeroFactura("FAC-00002")
                .fechaEmision(OffsetDateTime.now())
                .subtotal(BigDecimal.ZERO.setScale(2))
                .iva(BigDecimal.ZERO.setScale(2))
                .total(BigDecimal.ZERO.setScale(2))
                .metodoPago(MetodoPago.EFECTIVO)
                .estado(EstadoFactura.PAGADA)
                .ordenIngreso(ordenMock)
                .clienteFactura(propietarioMock)
                .usuario(cajeroMock)
                .build();

        when(ordenIngresoRepository.findById(ordenMock.getId())).thenReturn(Optional.of(ordenMock));
        when(facturaRepository.findByOrdenIngresoId(ordenMock.getId())).thenReturn(Optional.empty());
        when(usuarioRepository.findById(cajeroMock.getId())).thenReturn(Optional.of(cajeroMock));
        when(facturaRepository.count()).thenReturn(1L);
        when(facturaRepository.save(any(FacturaEntity.class))).thenReturn(facturaGuardada);

        when(clienteService.toDto(propietarioMock)).thenReturn(
                ClienteResponseDto.builder().numeroDocumento("1020304050").nombresRazonSocial("Mauricio Propietario").build()
        );
        when(ordenIngresoService.toDto(ordenMock)).thenReturn(
                OrdenIngresoResponseDto.builder()
                        .consecutivo(1L)
                        .vehiculo(VehiculoResponseDto.builder().placa("ABC123").categoria(CategoriaVehiculo.LIVIANO).build())
                        .build()
        );

        FacturaResponseDto response = facturaService.emitirFactura(request, cajeroMock.getId());

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO.setScale(2), response.getTotal());
        assertEquals(EstadoOrden.FACTURADO, ordenMock.getEstado());
    }
}
