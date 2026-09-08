package com.cdasanpedro.application.usecase.siigo;

import com.cdasanpedro.application.dto.siigo.SiigoStatusResponseDto;
import com.cdasanpedro.infrastructure.siigo.config.SiigoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiigoAuthServiceTest {

    @Mock
    private SiigoProperties properties;

    @Mock
    private RestClient.Builder restClientBuilder;

    @InjectMocks
    private SiigoAuthService authService;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getEnvironment()).thenReturn("SANDBOX");
        lenient().when(properties.getApiUrl()).thenReturn("https://api.siigo.com/v1");
        lenient().when(properties.isSandbox()).thenReturn(true);
    }

    @Test
    void testGetStatus_ModoSandbox() {
        when(properties.isConfigured()).thenReturn(false);

        SiigoStatusResponseDto status = authService.getStatus();

        assertNotNull(status);
        assertTrue(status.isConnected());
        assertEquals("SANDBOX", status.getEnvironment());
        assertTrue(status.getMessage().contains("Sandbox"));
    }

    @Test
    void testGetValidToken_SandboxFallback() {
        when(properties.isConfigured()).thenReturn(false);

        String token = authService.getValidToken();

        assertNotNull(token);
        assertTrue(token.startsWith("sandbox_simulated_token_"));
    }
}
