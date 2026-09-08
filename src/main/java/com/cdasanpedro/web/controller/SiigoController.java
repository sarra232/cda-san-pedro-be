package com.cdasanpedro.web.controller;

import com.cdasanpedro.application.dto.siigo.FacturaElectronicaResponseDto;
import com.cdasanpedro.application.dto.siigo.SiigoStatusResponseDto;
import com.cdasanpedro.application.usecase.siigo.SiigoAuthService;
import com.cdasanpedro.application.usecase.siigo.SiigoInvoiceService;
import com.cdasanpedro.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/siigo")
@RequiredArgsConstructor
public class SiigoController {

    private final SiigoInvoiceService invoiceService;
    private final SiigoAuthService authService;
    private final com.cdasanpedro.application.usecase.siigo.SiigoCustomerService customerService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SiigoStatusResponseDto>> getStatus() {
        SiigoStatusResponseDto status = authService.getStatus();
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    @PostMapping("/customers/sync-all")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<com.cdasanpedro.application.dto.siigo.SiigoSyncResultDto>> sincronizarClientesDesdeSiigo(
            @RequestParam(defaultValue = "20") int maxPages,
            @RequestParam(defaultValue = "100") int pageSize) {
        var result = customerService.importarClientesDesdeSiigo(maxPages, pageSize);
        return ResponseEntity.ok(ApiResponse.ok(result, "Proceso de sincronización finalizado"));
    }

    @GetMapping("/customers/search/{documento}")
    public ResponseEntity<ApiResponse<com.cdasanpedro.infrastructure.persistence.entity.ClienteEntity>> buscarClienteEnSiigo(
            @PathVariable String documento) {
        return customerService.buscarYAutoguardarDesdeSiigo(documento)
                .map(cliente -> ResponseEntity.ok(ApiResponse.ok(cliente, "Cliente encontrado y sincronizado desde SIIGO")))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(null, "No encontrado en SIIGO")));
    }

    @PostMapping("/facturas/{facturaId}/emitir")
    public ResponseEntity<ApiResponse<FacturaElectronicaResponseDto>> emitirFacturaDian(@PathVariable UUID facturaId) {
        FacturaElectronicaResponseDto response = invoiceService.emitirFacturaDian(facturaId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Factura electrónica procesada ante la DIAN vía SIIGO"));
    }

    @GetMapping("/facturas/{facturaId}")
    public ResponseEntity<ApiResponse<FacturaElectronicaResponseDto>> obtenerEstadoFiscal(@PathVariable UUID facturaId) {
        return invoiceService.obtenerPorFacturaId(facturaId)
                .map(dto -> ResponseEntity.ok(ApiResponse.ok(dto)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(null)));
    }

    @GetMapping("/facturas/{facturaId}/pdf")
    public ResponseEntity<byte[]> descargarPdfDian(@PathVariable UUID facturaId) {
        byte[] pdfBytes = invoiceService.obtenerFacturaPdfBytes(facturaId);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "Factura_DIAN_" + facturaId + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
