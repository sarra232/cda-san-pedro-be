package com.cdasanpedro.web.controller;

import com.cdasanpedro.application.dto.factura.FacturaRequestDto;
import com.cdasanpedro.application.dto.factura.FacturaResponseDto;
import com.cdasanpedro.application.usecase.factura.FacturaService;
import com.cdasanpedro.web.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;

    @PostMapping
    public ResponseEntity<ApiResponse<FacturaResponseDto>> emitirFactura(
            @Valid @RequestBody FacturaRequestDto request,
            Authentication authentication
    ) {
        UUID usuarioId = (UUID) authentication.getCredentials();
        FacturaResponseDto factura = facturaService.emitirFactura(request, usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(factura, "Factura " + factura.getNumeroFactura() + " emitida exitosamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FacturaResponseDto>>> listarFacturas(
            @RequestParam(name = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(name = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        List<FacturaResponseDto> facturas = facturaService.listarFacturas(fechaInicio, fechaFin);
        return ResponseEntity.ok(ApiResponse.ok(facturas, "Facturas recuperadas exitosamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FacturaResponseDto>> obtenerPorId(@PathVariable UUID id) {
        FacturaResponseDto factura = facturaService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.ok(factura, "Factura encontrada"));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable UUID id) {
        FacturaResponseDto factura = facturaService.obtenerPorId(id);
        byte[] pdfBytes = facturaService.generarPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "Factura_" + factura.getNumeroFactura() + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @PostMapping("/{id}/enviar")
    public ResponseEntity<ApiResponse<String>> enviarFacturaCliente(@PathVariable UUID id) {
        facturaService.enviarFacturaCliente(id);
        return ResponseEntity.ok(ApiResponse.ok("Factura enviada al cliente", "Notificación despachada"));
    }
}
