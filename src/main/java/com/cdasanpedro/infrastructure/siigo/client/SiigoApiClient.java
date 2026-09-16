package com.cdasanpedro.infrastructure.siigo.client;

import com.cdasanpedro.application.dto.siigo.*;
import com.cdasanpedro.infrastructure.siigo.config.SiigoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SiigoApiClient {

    private final SiigoProperties siigoProperties;
    private final RestClient.Builder restClientBuilder;

    private RestClient getClient(String bearerToken) {
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(java.time.Duration.ofSeconds(10));
        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(20));

        return restClientBuilder
                .requestFactory(requestFactory)
                .baseUrl(siigoProperties.getApiUrl())
                .defaultHeader("Partner-Id", "CDASanPedro")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public SiigoCustomerResponseDto createCustomer(SiigoCustomerRequestDto request, String token) {
        log.info(">> [SIIGO API] Sincronizando cliente documento: {}", request.getIdentification());
        return getClient(token)
                .post()
                .uri("/customers")
                .body(request)
                .retrieve()
                .body(SiigoCustomerResponseDto.class);
    }

    public SiigoCustomerListResponseDto getCustomers(int page, int pageSize, String token) {
        log.info(">> [SIIGO API] Consultando clientes en SIIGO (Página: {}, Tamaño: {})", page, pageSize);
        return getClient(token)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/customers")
                        .queryParam("page", page)
                        .queryParam("page_size", pageSize)
                        .build())
                .retrieve()
                .body(SiigoCustomerListResponseDto.class);
    }

    public SiigoCustomerListResponseDto getCustomerByIdentification(String identification, String token) {
        log.info(">> [SIIGO API] Buscando cliente por documento: {}", identification);
        return getClient(token)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/customers")
                        .queryParam("identification", identification.trim())
                        .build())
                .retrieve()
                .body(SiigoCustomerListResponseDto.class);
    }

    public SiigoInvoiceResponseDto createInvoice(SiigoInvoiceRequestDto request, String token) {
        log.info(">> [SIIGO API] Emitiendo Factura de Venta ante la DIAN para cliente: {}",
                request.getCustomer() != null ? request.getCustomer().getIdentification() : "N/A");
        return getClient(token)
                .post()
                .uri("/invoices")
                .body(request)
                .retrieve()
                .body(SiigoInvoiceResponseDto.class);
    }

    public SiigoInvoiceResponseDto getInvoice(String invoiceId, String token) {
        log.info(">> [SIIGO API] Consultando estado fiscal y timbrado DIAN de factura ID: {}", invoiceId);
        return getClient(token)
                .get()
                .uri("/invoices/{id}", invoiceId)
                .retrieve()
                .body(SiigoInvoiceResponseDto.class);
    }

    public SiigoInvoiceResponseDto stampInvoice(String invoiceId, String token) {
        log.info(">> [SIIGO API] Solicitando timbrado y transmisión DIAN para factura ID: {}", invoiceId);
        return getClient(token)
                .post()
                .uri("/invoices/{id}/stamp", invoiceId)
                .retrieve()
                .body(SiigoInvoiceResponseDto.class);
    }

    public SiigoInvoicePdfResponseDto getInvoicePdf(String invoiceId, String token) {
        log.info(">> [SIIGO API] Consultando PDF oficial de factura ID: {}", invoiceId);
        return getClient(token)
                .get()
                .uri("/invoices/{id}/pdf", invoiceId)
                .retrieve()
                .body(SiigoInvoicePdfResponseDto.class);
    }
}
