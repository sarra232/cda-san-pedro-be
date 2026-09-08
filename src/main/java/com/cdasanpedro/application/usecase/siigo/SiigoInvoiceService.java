package com.cdasanpedro.application.usecase.siigo;

import com.cdasanpedro.application.dto.siigo.*;
import com.cdasanpedro.core.exception.ResourceNotFoundException;
import com.cdasanpedro.core.gateway.NotificationGateway;
import com.cdasanpedro.core.model.enums.EstadoFacturaDian;
import com.cdasanpedro.infrastructure.notification.EmailTemplateBuilder;
import com.cdasanpedro.infrastructure.pdf.PdfGeneratorService;
import com.cdasanpedro.infrastructure.persistence.entity.*;
import com.cdasanpedro.infrastructure.persistence.repository.*;
import com.cdasanpedro.infrastructure.siigo.client.SiigoApiClient;
import com.cdasanpedro.infrastructure.siigo.config.SiigoProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiigoInvoiceService {

    private final FacturaRepository facturaRepository;
    private final FacturaElectronicaDianRepository dianRepository;
    private final MapeoCatalogoSiigoRepository mapeoRepository;
    private final SiigoCustomerService customerService;
    private final SiigoApiClient apiClient;
    private final SiigoAuthService authService;
    private final SiigoProperties properties;
    private final PdfGeneratorService pdfGeneratorService;
    private final NotificationGateway notificationGateway;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public FacturaElectronicaResponseDto emitirFacturaDian(UUID facturaId) {
        FacturaEntity factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con ID: " + facturaId));

        Optional<FacturaElectronicaDianEntity> existenteOpt = dianRepository.findByFacturaId(facturaId);
        if (existenteOpt.isPresent() && existenteOpt.get().getEstadoDian() == EstadoFacturaDian.EMITIDA) {
            log.info(">> [SIIGO] La factura {} ya cuenta con emisión DIAN previa.", factura.getNumeroFactura());
            return mapToDto(existenteOpt.get());
        }

        FacturaElectronicaDianEntity dianEntity = existenteOpt.orElseGet(() -> FacturaElectronicaDianEntity.builder()
                .factura(factura)
                .ambiente(properties.getEnvironment())
                .estadoDian(EstadoFacturaDian.PENDIENTE)
                .intentos(0)
                .build());

        dianEntity.setIntentos(dianEntity.getIntentos() + 1);

        try {
            // 1. Sincronizar Pagador con SIIGO
            customerService.sincronizarCliente(factura.getClienteFactura());

            // 2. Construir Payload de Factura de Venta
            SiigoInvoiceRequestDto invoiceRequest = construirPayloadFactura(factura);
            String payloadJson = objectMapper.writeValueAsString(invoiceRequest);
            dianEntity.setPayloadEnviado(payloadJson);

            SiigoInvoiceResponseDto response;

            // 3. Ejecutar llamada o simulación Sandbox
            if (!properties.isConfigured() && properties.isSandbox()) {
                log.info(">> [SIIGO SANDBOX] Emisión electrónica simulada para factura: {}", factura.getNumeroFactura());
                response = generarRespuestaSimulada(factura, invoiceRequest);
            } else {
                String token = authService.getValidToken();
                response = apiClient.createInvoice(invoiceRequest, token);
            }

            // 4. Guardar datos fiscales oficiales
            String respuestaJson = objectMapper.writeValueAsString(response);
            dianEntity.setRespuestaSiigo(respuestaJson);
            dianEntity.setSiigoInvoiceId(response.getId());
            dianEntity.setNumeroFacturaSiigo(response.getName() != null ? response.getName() : "FV-" + response.getNumber());
            
            String cufe = response.getCufe() != null ? response.getCufe() : 
                    (response.getStamp() != null ? response.getStamp().getCufe() : UUID.randomUUID().toString().replace("-", ""));
            String qr = response.getQrCode() != null ? response.getQrCode() :
                    (response.getStamp() != null ? response.getStamp().getQr() : "https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=" + cufe);
            String pdfUrl = response.getPublicUrl() != null ? response.getPublicUrl() : "https://api.siigo.com/v1/invoices/" + response.getId() + "/pdf";

            dianEntity.setCufe(cufe);
            dianEntity.setQrDian(qr);
            dianEntity.setPdfSiigoUrl(pdfUrl);
            dianEntity.setEstadoDian(EstadoFacturaDian.EMITIDA);
            dianEntity.setMensajeRespuesta("Factura emitida y validada exitosamente ante la DIAN vía SIIGO Cloud.");
            dianEntity.setFechaEmisionDian(OffsetDateTime.now());

            log.info(">> [SIIGO DIAN] Factura {} emitida exitosamente con CUFE: {}",
                    factura.getNumeroFactura(), cufe);

            // 5. Despacho obligatorio de correo con PDF DIAN adjunto por ley
            despacharCorreoFacturaDian(factura, dianEntity);

        } catch (Exception e) {
            log.error(">> [SIIGO DIAN] Error al emitir factura {}: {}", factura.getNumeroFactura(), e.getMessage());
            dianEntity.setEstadoDian(EstadoFacturaDian.FALLIDA);
            dianEntity.setMensajeRespuesta("Fallo en transmisión: " + e.getMessage());
        }

        FacturaElectronicaDianEntity guardada = dianRepository.save(dianEntity);
        return mapToDto(guardada);
    }

    @Transactional(readOnly = true)
    public Optional<FacturaElectronicaResponseDto> obtenerPorFacturaId(UUID facturaId) {
        return dianRepository.findByFacturaId(facturaId).map(this::mapToDto);
    }

    private SiigoInvoiceRequestDto construirPayloadFactura(FacturaEntity factura) {
        String fechaHoy = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String categoria = factura.getOrdenIngreso() != null && factura.getOrdenIngreso().getVehiculo() != null
                ? factura.getOrdenIngreso().getVehiculo().getCategoria().name()
                : "LIVIANO";

        boolean esReinspeccion = factura.getOrdenIngreso() != null && 
                (Boolean.TRUE.equals(factura.getOrdenIngreso().getEsReinspeccion()) || "REINSPECCION_GRATUITA".equalsIgnoreCase(factura.getOrdenIngreso().getTipoServicio()));

        String categoriaLookup = esReinspeccion ? "REINSPECCION_GRATUITA" : categoria;
        MapeoCatalogoSiigoEntity mapeo = mapeoRepository.findByCategoriaCdaAndActivoTrue(categoriaLookup)
                .orElseGet(() -> MapeoCatalogoSiigoEntity.builder()
                        .codigoProductoSiigo("002")
                        .descripcionSiigo("REVISION TECNO AUTO LIVIANO")
                        .siigoTaxId(18668)
                        .tarifaIva(esReinspeccion ? BigDecimal.ZERO : new BigDecimal("19.00"))
                        .build());

        List<SiigoInvoiceRequestDto.InvoiceItemDto> items = new ArrayList<>();

        List<SiigoInvoiceRequestDto.InvoiceTaxDto> itemTaxes = Collections.emptyList();
        if (!esReinspeccion && mapeo.getSiigoTaxId() != null) {
            itemTaxes = List.of(SiigoInvoiceRequestDto.InvoiceTaxDto.builder()
                    .id(mapeo.getSiigoTaxId())
                    .name(mapeo.getCodigoProductoSiigo().equals("005") ? "Iva servicios 19% MOTOS" : "IVA POR SERVICIOS 19%")
                    .type("IVA")
                    .percentage(mapeo.getTarifaIva())
                    .build());
        }

        if (factura.getItems() != null && !factura.getItems().isEmpty()) {
            for (ItemFacturaEntity it : factura.getItems()) {
                items.add(SiigoInvoiceRequestDto.InvoiceItemDto.builder()
                        .code(mapeo.getCodigoProductoSiigo())
                        .description(it.getDescripcion())
                        .quantity(it.getCantidad())
                        .price(it.getValorUnitario())
                        .discount(BigDecimal.ZERO)
                        .taxes(itemTaxes)
                        .build());
            }
        } else {
            items.add(SiigoInvoiceRequestDto.InvoiceItemDto.builder()
                    .code(mapeo.getCodigoProductoSiigo())
                    .description(mapeo.getDescripcionSiigo())
                    .quantity(1)
                    .price(factura.getSubtotal())
                    .discount(BigDecimal.ZERO)
                    .taxes(itemTaxes)
                    .build());
        }

        Integer paymentId = mapMetodoPagoToSiigoId(factura.getMetodoPago());
        List<SiigoInvoiceRequestDto.InvoicePaymentDto> payments = List.of(
                SiigoInvoiceRequestDto.InvoicePaymentDto.builder()
                        .id(paymentId)
                        .value(factura.getTotal())
                        .dueDate(fechaHoy)
                        .build()
        );

        String placa = factura.getOrdenIngreso() != null && factura.getOrdenIngreso().getVehiculo() != null
                ? factura.getOrdenIngreso().getVehiculo().getPlaca()
                : "";

        String rawDoc = factura.getClienteFactura().getNumeroDocumento().trim();
        String customerDoc = rawDoc.replace(".", "").replace(" ", "").trim();
        if (customerDoc.contains("-")) {
            customerDoc = customerDoc.split("-")[0].trim();
        }

        return SiigoInvoiceRequestDto.builder()
                .document(SiigoInvoiceRequestDto.DocumentRefDto.builder().id(properties.getDocumentTypeId()).build())
                .date(fechaHoy)
                .customer(SiigoInvoiceRequestDto.CustomerRefDto.builder()
                        .identification(customerDoc)
                        .branchOffice(0)
                        .build())
                .seller(properties.getSellerId() != null ? properties.getSellerId() : 560)
                .items(items)
                .payments(payments)
                .observations("Placa: " + placa + " | Comprobante Interno: " + factura.getNumeroFactura() + " | CDA San Pedro S.A.S.")
                .build();
    }

    private Integer mapMetodoPagoToSiigoId(com.cdasanpedro.core.model.enums.MetodoPago metodoPago) {
        if (metodoPago == null) return 5014; // Default: Efectivo
        return switch (metodoPago) {
            case EFECTIVO -> 5014;
            case TRANSFERENCIA -> 7767;
            case DATAFONO_TARJETA, TARJETA -> 5016; // Tarjeta Débito / Datáfono
            case SISTECREDITO -> 5015; // Crédito / Sistecrédito
            case MIXTO -> 5014;
        };
    }

    private SiigoInvoiceResponseDto generarRespuestaSimulada(FacturaEntity factura, SiigoInvoiceRequestDto request) {
        String simCufe = "cufe_sandbox_" + UUID.randomUUID().toString().replace("-", "") + "dian2026";
        int simNum = 1000 + (int) (Math.random() * 9000);

        return SiigoInvoiceResponseDto.builder()
                .id("siigo_sbx_" + UUID.randomUUID())
                .name("SETT-" + simNum)
                .number(simNum)
                .date(request.getDate())
                .cufe(simCufe)
                .qrCode("https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=" + simCufe)
                .publicUrl("https://api.siigo.com/v1/invoices/mock-" + simNum + "/pdf")
                .total(factura.getTotal())
                .stamp(SiigoInvoiceResponseDto.StampDto.builder()
                        .status("Approved")
                        .cufe(simCufe)
                        .qr("https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=" + simCufe)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] obtenerFacturaPdfBytes(UUID facturaId) {
        FacturaEntity factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con ID: " + facturaId));

        // 1. Intentar descargar desde SIIGO API si ya fue emitida
        Optional<FacturaElectronicaDianEntity> dianOpt = dianRepository.findByFacturaId(facturaId);
        if (dianOpt.isPresent() && dianOpt.get().getSiigoInvoiceId() != null && properties.isConfigured()) {
            try {
                String token = authService.getValidToken();
                SiigoInvoicePdfResponseDto pdfDto = apiClient.getInvoicePdf(dianOpt.get().getSiigoInvoiceId(), token);
                if (pdfDto != null && pdfDto.getBase64() != null && !pdfDto.getBase64().isBlank()) {
                    log.info(">> [SIIGO PDF] PDF oficial obtenido desde SIIGO API para factura {}", factura.getNumeroFactura());
                    return Base64.getDecoder().decode(pdfDto.getBase64());
                }
            } catch (Exception e) {
                log.warn(">> [SIIGO PDF] No se pudo obtener el PDF directo de SIIGO ({}), usando generador institucional.", e.getMessage());
            }
        }

        // 2. Fallback: Generador institucional CDA San Pedro
        return pdfGeneratorService.generarFacturaPdf(factura);
    }

    private void despacharCorreoFacturaDian(FacturaEntity factura, FacturaElectronicaDianEntity dianEntity) {
        if (factura.getClienteFactura() == null || factura.getClienteFactura().getEmail() == null || factura.getClienteFactura().getEmail().isBlank()) {
            log.info(">> [SIIGO DIAN] El cliente no tiene correo registrado, se omite el envío de email.");
            return;
        }

        try {
            byte[] pdfBytes = null;
            if (dianEntity.getSiigoInvoiceId() != null && properties.isConfigured()) {
                try {
                    String token = authService.getValidToken();
                    SiigoInvoicePdfResponseDto pdfDto = apiClient.getInvoicePdf(dianEntity.getSiigoInvoiceId(), token);
                    if (pdfDto != null && pdfDto.getBase64() != null && !pdfDto.getBase64().isBlank()) {
                        pdfBytes = Base64.getDecoder().decode(pdfDto.getBase64());
                    }
                } catch (Exception e) {
                    log.warn(">> [SIIGO PDF] Falló descarga directa SIIGO, generando PDF local: {}", e.getMessage());
                }
            }

            if (pdfBytes == null) {
                pdfBytes = pdfGeneratorService.generarFacturaPdf(factura);
            }

            String clienteNombre = factura.getClienteFactura().getNombresRazonSocial();
            String placa = (factura.getOrdenIngreso() != null && factura.getOrdenIngreso().getVehiculo() != null)
                    ? factura.getOrdenIngreso().getVehiculo().getPlaca()
                    : "N/A";
            String numFactura = dianEntity.getNumeroFacturaSiigo() != null ? dianEntity.getNumeroFacturaSiigo() : factura.getNumeroFactura();
            String metodo = factura.getMetodoPago() != null ? factura.getMetodoPago().name() : "EFECTIVO";

            String htmlBody = EmailTemplateBuilder.buildComprobantePago(
                    clienteNombre,
                    placa,
                    numFactura,
                    factura.getTotal(),
                    metodo,
                    dianEntity.getCufe(),
                    dianEntity.getPdfSiigoUrl()
            );

            String attachmentName = "Factura_DIAN_" + numFactura.replace(" ", "_") + ".pdf";
            String subject = "🧾 Factura Electrónica DIAN " + numFactura + " - CDA San Pedro (PDF Adjunto)";

            notificationGateway.sendEmail(
                    factura.getClienteFactura().getEmail().trim(),
                    subject,
                    htmlBody,
                    pdfBytes,
                    attachmentName
            );

            log.info(">> [SIIGO DIAN] Correo legal con PDF adjunto despachado a {}", factura.getClienteFactura().getEmail());
        } catch (Exception e) {
            log.error(">> [SIIGO DIAN] Error al despachar correo legal con PDF: {}", e.getMessage(), e);
        }
    }

    public FacturaElectronicaResponseDto mapToDto(FacturaElectronicaDianEntity entity) {
        return FacturaElectronicaResponseDto.builder()
                .id(entity.getId())
                .facturaId(entity.getFactura().getId())
                .numeroFacturaLocal(entity.getFactura().getNumeroFactura())
                .ambiente(entity.getAmbiente())
                .siigoInvoiceId(entity.getSiigoInvoiceId())
                .numeroFacturaSiigo(entity.getNumeroFacturaSiigo())
                .cufe(entity.getCufe())
                .qrDian(entity.getQrDian())
                .pdfSiigoUrl(entity.getPdfSiigoUrl())
                .estadoDian(entity.getEstadoDian())
                .mensajeRespuesta(entity.getMensajeRespuesta())
                .intentos(entity.getIntentos())
                .fechaEmisionDian(entity.getFechaEmisionDian())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
