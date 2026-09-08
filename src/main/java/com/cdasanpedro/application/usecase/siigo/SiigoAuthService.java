package com.cdasanpedro.application.usecase.siigo;

import com.cdasanpedro.application.dto.siigo.SiigoAuthRequestDto;
import com.cdasanpedro.application.dto.siigo.SiigoAuthResponseDto;
import com.cdasanpedro.application.dto.siigo.SiigoStatusResponseDto;
import com.cdasanpedro.infrastructure.siigo.config.SiigoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiigoAuthService {

    private final SiigoProperties siigoProperties;
    private final RestClient.Builder restClientBuilder;

    private String cachedToken;
    private OffsetDateTime tokenExpiration;

    public synchronized String getValidToken() {
        if (cachedToken != null && tokenExpiration != null && OffsetDateTime.now().isBefore(tokenExpiration.minusMinutes(5))) {
            return cachedToken;
        }

        if (!siigoProperties.isConfigured()) {
            if (siigoProperties.isSandbox()) {
                log.warn(">> [SIIGO] Credenciales no configuradas. Usando token simulado para ambiente Sandbox.");
                cachedToken = "sandbox_simulated_token_" + System.currentTimeMillis();
                tokenExpiration = OffsetDateTime.now().plusHours(8);
                return cachedToken;
            }
            throw new IllegalStateException("Las credenciales de SIIGO (SIIGO_USERNAME / SIIGO_ACCESS_KEY) no están configuradas.");
        }

        try {
            String authBaseUrl = siigoProperties.getApiUrl().contains("/v1")
                    ? siigoProperties.getApiUrl().replace("/v1", "")
                    : siigoProperties.getApiUrl();
            RestClient client = restClientBuilder.baseUrl(authBaseUrl).build();
            SiigoAuthRequestDto authRequest = SiigoAuthRequestDto.builder()
                    .username(siigoProperties.getUsername())
                    .accessKey(siigoProperties.getAccessKey())
                    .build();

            SiigoAuthResponseDto response = client.post()
                    .uri("/auth")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Partner-Id", "CDASanPedro")
                    .body(authRequest)
                    .retrieve()
                    .body(SiigoAuthResponseDto.class);

            if (response != null && response.getAccessToken() != null) {
                cachedToken = response.getAccessToken();
                long expiresInSeconds = response.getExpiresIn() != null ? response.getExpiresIn() : 86400L;
                tokenExpiration = OffsetDateTime.now().plusSeconds(expiresInSeconds);
                log.info(">> [SIIGO] Autenticación exitosa en ambiente: {}. Token válido por {}s.",
                        siigoProperties.getEnvironment(), expiresInSeconds);
                return cachedToken;
            } else {
                throw new IllegalStateException("Respuesta de autenticación vacía desde SIIGO API.");
            }
        } catch (Exception e) {
            log.error(">> [SIIGO] Error al autenticar con SIIGO API: {}", e.getMessage());
            if (siigoProperties.isSandbox()) {
                log.warn(">> [SIIGO] Fallback activo para Sandbox. Generando sesión simulada.");
                cachedToken = "sandbox_mock_token_" + System.currentTimeMillis();
                tokenExpiration = OffsetDateTime.now().plusHours(8);
                return cachedToken;
            }
            throw new IllegalStateException("No fue posible autenticar con SIIGO: " + e.getMessage(), e);
        }
    }

    public SiigoStatusResponseDto getStatus() {
        boolean configured = siigoProperties.isConfigured();
        boolean connected = false;
        String message = "Sin configurar";

        if (configured) {
            try {
                String token = getValidToken();
                connected = token != null && !token.isBlank();
                message = connected ? "Conexión activa con SIIGO API (" + siigoProperties.getEnvironment() + ")" : "Error de autenticación";
            } catch (Exception e) {
                connected = false;
                message = "Error: " + e.getMessage();
            }
        } else if (siigoProperties.isSandbox()) {
            connected = true;
            message = "Modo Simulación Sandbox Activo (Sin credenciales de producción)";
        }

        return SiigoStatusResponseDto.builder()
                .configured(configured)
                .environment(siigoProperties.getEnvironment())
                .apiUrl(siigoProperties.getApiUrl())
                .username(siigoProperties.getUsername() != null ? maskEmail(siigoProperties.getUsername()) : null)
                .documentTypeId(siigoProperties.getDocumentTypeId())
                .connected(connected)
                .message(message)
                .build();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
