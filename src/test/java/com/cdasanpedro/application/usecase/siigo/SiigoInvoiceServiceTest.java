package com.cdasanpedro.application.usecase.siigo;

import com.cdasanpedro.application.dto.siigo.FacturaElectronicaResponseDto;
import com.cdasanpedro.application.dto.siigo.SiigoInvoiceResponseDto;
import com.cdasanpedro.core.model.enums.*;
import com.cdasanpedro.infrastructure.persistence.entity.*;
import com.cdasanpedro.infrastructure.persistence.repository.*;
import com.cdasanpedro.infrastructure.siigo.client.SiigoApiClient;
import com.cdasanpedro.infrastructure.siigo.config.SiigoProperties;
import org.junit.jupiter.api.BeforeEach;
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
class SiigoInvoiceServiceTest {

    @Mock
    private FacturaRepository facturaRepository;

    @Mock
    private FacturaElectronicaDianRepository dianRepository;

    @Mock
    private MapeoCatalogoSiigoRepository mapeoRepository;

    @Mock
    private SiigoCustomerService customerService;

    @Mock
    private SiigoApiClient apiClient;

    @Mock
    private SiigoAuthService authService;

    @Mock
    private SiigoProperties properties;

    @InjectMocks
    private SiigoInvoiceService invoiceService;

    private FacturaEntity factura;
    private ClienteEntity cliente;
    private VehiculoEntity vehiculo;
    private OrdenIngresoEntity orden;

    @BeforeEach
    void setUp() {
        cliente = ClienteEntity.builder()
                .id(UUID.randomUUID())
                .tipoDocumento(TipoDocumento.CC)
                .numeroDocumento("1020304050")
                .nombresRazonSocial("Mauricio Cliente")
                .celular("3001234567")
                .build();

        vehiculo = VehiculoEntity.builder()
                .id(UUID.randomUUID())
                .placa("ABC123")
                .categoria(CategoriaVehiculo.LIVIANO)
                .marca("CHEVROLET")
                .linea("SAIL")
                .propietario(cliente)
                .build();

        orden = OrdenIngresoEntity.builder()
                .id(UUID.randomUUID())
                .consecutivo(101L)
                .vehiculo(vehiculo)
                .conductor(cliente)
                .tipoServicio("RTM_LEGAL")
                .build();

        factura = FacturaEntity.builder()
                .id(UUID.randomUUID())
                .numeroFactura("FAC-00101")
                .consecutivo(101L)
                .subtotal(new BigDecimal("268907.56"))
                .iva(new BigDecimal("51092.44"))
                .total(new BigDecimal("320000.00"))
                .metodoPago(MetodoPago.EFECTIVO)
                .estado(EstadoFactura.PAGADA)
                .clienteFactura(cliente)
                .ordenIngreso(orden)
                .build();

        lenient().when(properties.getDocumentTypeId()).thenReturn(24581);
        lenient().when(properties.getEnvironment()).thenReturn("SANDBOX");
        lenient().when(properties.isSandbox()).thenReturn(true);
    }

    @Test
    void testEmitirFacturaDian_EmisionSimuladaSandbox() {
        when(facturaRepository.findById(factura.getId())).thenReturn(Optional.of(factura));
        when(dianRepository.findByFacturaId(factura.getId())).thenReturn(Optional.empty());
        when(mapeoRepository.findByCategoriaCdaAndActivoTrue("LIVIANO")).thenReturn(Optional.of(
                MapeoCatalogoSiigoEntity.builder().codigoProductoSiigo("RTM-LIV-01").descripcionSiigo("RTM Livianos").build()
        ));

        when(dianRepository.save(any(FacturaElectronicaDianEntity.class))).thenAnswer(invocation -> {
            FacturaElectronicaDianEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        FacturaElectronicaResponseDto response = invoiceService.emitirFacturaDian(factura.getId());

        assertNotNull(response);
        assertEquals(EstadoFacturaDian.EMITIDA, response.getEstadoDian());
        assertNotNull(response.getCufe());
        assertTrue(response.getCufe().startsWith("cufe_sandbox_"));
        assertNotNull(response.getPdfSiigoUrl());
        verify(customerService, times(1)).sincronizarCliente(cliente);
    }

    @Test
    void testSincronizarEstadoDian_FacturaExistente() {
        FacturaElectronicaDianEntity dianEntity = FacturaElectronicaDianEntity.builder()
                .id(UUID.randomUUID())
                .factura(factura)
                .siigoInvoiceId("siigo-doc-123")
                .estadoDian(EstadoFacturaDian.PENDIENTE)
                .build();

        when(facturaRepository.findById(factura.getId())).thenReturn(Optional.of(factura));
        when(dianRepository.findByFacturaId(factura.getId())).thenReturn(Optional.of(dianEntity));
        when(properties.isConfigured()).thenReturn(true);
        when(authService.getValidToken()).thenReturn("mock-token");

        SiigoInvoiceResponseDto apiResponse = SiigoInvoiceResponseDto.builder()
                .id("siigo-doc-123")
                .cufe("cufe-dian-aprobado-999")
                .qrCode("https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=cufe-dian-aprobado-999")
                .publicUrl("https://api.siigo.com/v1/invoices/siigo-doc-123/pdf")
                .stamp(SiigoInvoiceResponseDto.StampDto.builder().status("Approved").cufe("cufe-dian-aprobado-999").build())
                .build();

        when(apiClient.getInvoice("siigo-doc-123", "mock-token")).thenReturn(apiResponse);
        when(dianRepository.save(any(FacturaElectronicaDianEntity.class))).thenAnswer(i -> i.getArgument(0));

        FacturaElectronicaResponseDto res = invoiceService.sincronizarEstadoDian(factura.getId());

        assertNotNull(res);
        assertEquals(EstadoFacturaDian.EMITIDA, res.getEstadoDian());
        assertEquals("cufe-dian-aprobado-999", res.getCufe());
        assertTrue(res.getMensajeRespuesta().contains("Aprobada y validada por la DIAN"));
    }
}
