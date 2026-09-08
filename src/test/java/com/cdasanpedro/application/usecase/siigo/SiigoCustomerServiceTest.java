package com.cdasanpedro.application.usecase.siigo;

import com.cdasanpedro.application.dto.siigo.SiigoCustomerResponseDto;
import com.cdasanpedro.core.model.enums.TipoDocumento;
import com.cdasanpedro.infrastructure.persistence.entity.ClienteEntity;
import com.cdasanpedro.infrastructure.siigo.client.SiigoApiClient;
import com.cdasanpedro.infrastructure.siigo.config.SiigoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiigoCustomerServiceTest {

    @Mock
    private SiigoApiClient apiClient;

    @Mock
    private SiigoAuthService authService;

    @Mock
    private SiigoProperties properties;

    @Mock
    private com.cdasanpedro.infrastructure.persistence.repository.ClienteRepository clienteRepository;

    @Mock
    private com.cdasanpedro.infrastructure.persistence.repository.TerceroRepository terceroRepository;

    @InjectMocks
    private SiigoCustomerService customerService;

    private ClienteEntity cliente;

    @BeforeEach
    void setUp() {
        cliente = ClienteEntity.builder()
                .id(UUID.randomUUID())
                .tipoDocumento(TipoDocumento.CC)
                .numeroDocumento("1020304050")
                .nombresRazonSocial("Mauricio Cliente")
                .celular("3001234567")
                .email("mauricio@example.com")
                .build();
    }

    @Test
    void testMapTipoDocumento() {
        assertEquals("13", customerService.mapTipoDocumento(TipoDocumento.CC));
        assertEquals("31", customerService.mapTipoDocumento(TipoDocumento.NIT));
        assertEquals("22", customerService.mapTipoDocumento(TipoDocumento.CE));
        assertEquals("41", customerService.mapTipoDocumento(TipoDocumento.PASAPORTE));
    }

    @Test
    void testSincronizarCliente_Sandbox() {
        when(properties.isConfigured()).thenReturn(false);
        when(properties.isSandbox()).thenReturn(true);

        SiigoCustomerResponseDto response = customerService.sincronizarCliente(cliente);

        assertNotNull(response);
        assertEquals("1020304050", response.getIdentification());
        assertEquals("13", response.getIdType());
        assertTrue(response.getId().contains("sandbox_cust_"));
    }

    @Test
    void testImportarClientesDesdeSiigo_Sandbox() {
        when(properties.isConfigured()).thenReturn(false);
        when(properties.isSandbox()).thenReturn(true);

        var result = customerService.importarClientesDesdeSiigo(1, 10);

        assertNotNull(result);
        assertTrue(result.getTotalProcesados() >= 3);
        assertTrue(result.getMensaje().contains("completada"));
    }
}
