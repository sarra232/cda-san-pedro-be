package com.cdasanpedro.application.usecase.siigo;

import com.cdasanpedro.application.dto.siigo.*;
import com.cdasanpedro.core.exception.BusinessException;
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
import java.math.RoundingMode;
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
    private final com.cdasanpedro.application.usecase.notificacion.NotificacionService notificacionService;
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

            // 2. Construir Payload de Factura de Venta con emisión DIAN habilitada (stamp.send: true, mail.send: true)
            SiigoInvoiceRequestDto invoiceRequest = construirPayloadFactura(factura);
            String payloadJson = objectMapper.writeValueAsString(invoiceRequest);
            dianEntity.setPayloadEnviado(payloadJson);

            SiigoInvoiceResponseDto response;

            // 3. Ejecutar llamada o simulación Sandbox
            if (!properties.isConfigured() && properties.isSandbox()) {
                log.info(">> [SIIGO SANDBOX] Emisión electrónica simulada con aprobación DIAN para factura: {}", factura.getNumeroFactura());
                response = generarRespuestaSimulada(factura, invoiceRequest);
            } else {
                String token = authService.getValidToken();
                response = apiClient.createInvoice(invoiceRequest, token);

                // Si la DIAN está procesando de forma asíncrona, consultar estado final tras breve espera
                if (response != null && response.getId() != null && 
                        (response.getCufe() == null || (response.getStamp() != null && "In_process".equalsIgnoreCase(response.getStamp().getStatus())))) {
                    try {
                        Thread.sleep(2000); // 2 segundos para dar margen de validación a la DIAN
                        SiigoInvoiceResponseDto updated = apiClient.getInvoice(response.getId(), token);
                        if (updated != null) {
                            response = updated;
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } catch (Exception exPoll) {
                        log.warn(">> [SIIGO DIAN] No se pudo re-consultar estado inmediato: {}", exPoll.getMessage());
                    }
                }
            }

            // 4. Guardar datos fiscales oficiales
            String respuestaJson = objectMapper.writeValueAsString(response);
            dianEntity.setRespuestaSiigo(respuestaJson);
            dianEntity.setSiigoInvoiceId(response.getId());
            dianEntity.setNumeroFacturaSiigo(response.getName() != null ? response.getName() : "FV-" + response.getNumber());
            
            String cufe = response.getCufe() != null ? response.getCufe() : 
                    (response.getStamp() != null && response.getStamp().getCufe() != null ? response.getStamp().getCufe() : null);
            String qr = response.getQrCode() != null ? response.getQrCode() :
                    (response.getStamp() != null && response.getStamp().getQr() != null ? response.getStamp().getQr() : null);
            String pdfUrl = response.getPublicUrl() != null ? response.getPublicUrl() : "https://api.siigo.com/v1/invoices/" + response.getId() + "/pdf";

            String stampStatus = response.getStamp() != null ? response.getStamp().getStatus() : null;

            if ("Rejected".equalsIgnoreCase(stampStatus)) {
                dianEntity.setEstadoDian(EstadoFacturaDian.FALLIDA);
                String errorDetails = extraerErroresSiigo(response);
                dianEntity.setMensajeRespuesta("Rechazada por la DIAN vía SIIGO: " + errorDetails);
                log.warn(">> [SIIGO DIAN] Factura {} rechazada por DIAN: {}", factura.getNumeroFactura(), errorDetails);
            } else {
                if (cufe == null || cufe.isBlank()) {
                    cufe = properties.isSandbox() ? UUID.randomUUID().toString().replace("-", "") : null;
                }
                if (qr == null && cufe != null) {
                    qr = "https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=" + cufe;
                }

                dianEntity.setCufe(cufe);
                dianEntity.setQrDian(qr);
                dianEntity.setPdfSiigoUrl(pdfUrl);
                dianEntity.setEstadoDian(EstadoFacturaDian.EMITIDA);
                dianEntity.setMensajeRespuesta("Factura emitida y transmitida a la DIAN vía SIIGO Cloud (Estado DIAN: " + (stampStatus != null ? stampStatus : "Aprobada") + ")");
                dianEntity.setFechaEmisionDian(OffsetDateTime.now());

                log.info(">> [SIIGO DIAN] Factura {} emitida y enviada a DIAN exitosamente con CUFE: {}",
                        factura.getNumeroFactura(), cufe);

                // 5. Despacho obligatorio de correo con PDF DIAN adjunto por ley (solo tras emisión exitosa)
                despacharCorreoFacturaDian(factura, dianEntity);

                // 6. Despacho de WhatsApp con datos oficiales de SIIGO
                despacharWhatsAppFacturaDian(factura, dianEntity);
            }

        } catch (Exception e) {
            log.error(">> [SIIGO DIAN] Error al emitir factura {}: {}", factura.getNumeroFactura(), e.getMessage());
            dianEntity.setEstadoDian(EstadoFacturaDian.FALLIDA);
            
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Error desconocido en comunicación con SIIGO";
            if (errorMsg.contains("timed out") || errorMsg.contains("Timeout") || errorMsg.contains("SocketTimeoutException")) {
                errorMsg = "Tiempo de espera agotado al comunicar con SIIGO Cloud (Timeout). Puede reintentar la emisión fiscal.";
            }
            dianEntity.setMensajeRespuesta("Fallo en transmisión a la DIAN: " + errorMsg);
        }

        FacturaElectronicaDianEntity guardada = dianRepository.save(dianEntity);
        return mapToDto(guardada);
    }

    @Transactional
    public FacturaElectronicaResponseDto sincronizarEstadoDian(UUID facturaId) {
        FacturaEntity factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con ID: " + facturaId));

        FacturaElectronicaDianEntity dianEntity = dianRepository.findByFacturaId(facturaId)
                .orElseThrow(() -> new BusinessException("La factura " + factura.getNumeroFactura() + " no tiene registro de emisión previa en SIIGO."));

        if (dianEntity.getSiigoInvoiceId() == null || !properties.isConfigured()) {
            return mapToDto(dianEntity);
        }

        try {
            String token = authService.getValidToken();
            // 1. Consultar estado en SIIGO
            SiigoInvoiceResponseDto invoiceInfo = apiClient.getInvoice(dianEntity.getSiigoInvoiceId(), token);
            
            // 2. Si no tiene CUFE o status es Draft, solicitar timbrado expreso
            if (invoiceInfo != null && (invoiceInfo.getCufe() == null || (invoiceInfo.getStamp() != null && "Draft".equalsIgnoreCase(invoiceInfo.getStamp().getStatus())))) {
                try {
                    log.info(">> [SIIGO DIAN] Solicitando timbrado explícito ante DIAN para factura SIIGO ID: {}", dianEntity.getSiigoInvoiceId());
                    invoiceInfo = apiClient.stampInvoice(dianEntity.getSiigoInvoiceId(), token);
                } catch (Exception exStamp) {
                    log.warn(">> [SIIGO DIAN] Intento de timbrado retornó: {}", exStamp.getMessage());
                }
            }

            if (invoiceInfo != null) {
                String respuestaJson = objectMapper.writeValueAsString(invoiceInfo);
                dianEntity.setRespuestaSiigo(respuestaJson);

                String cufe = invoiceInfo.getCufe() != null ? invoiceInfo.getCufe() :
                        (invoiceInfo.getStamp() != null ? invoiceInfo.getStamp().getCufe() : dianEntity.getCufe());
                String qr = invoiceInfo.getQrCode() != null ? invoiceInfo.getQrCode() :
                        (invoiceInfo.getStamp() != null ? invoiceInfo.getStamp().getQr() : dianEntity.getQrDian());
                String pdfUrl = invoiceInfo.getPublicUrl() != null ? invoiceInfo.getPublicUrl() : dianEntity.getPdfSiigoUrl();
                String stampStatus = invoiceInfo.getStamp() != null ? invoiceInfo.getStamp().getStatus() : null;

                if (cufe != null && !cufe.isBlank()) {
                    dianEntity.setCufe(cufe);
                    dianEntity.setQrDian(qr != null ? qr : "https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=" + cufe);
                    dianEntity.setPdfSiigoUrl(pdfUrl);
                    dianEntity.setEstadoDian(EstadoFacturaDian.EMITIDA);
                    dianEntity.setMensajeRespuesta("Aprobada y validada por la DIAN (CUFE: " + cufe + ")");
                } else if ("Rejected".equalsIgnoreCase(stampStatus)) {
                    dianEntity.setEstadoDian(EstadoFacturaDian.FALLIDA);
                    dianEntity.setMensajeRespuesta("Rechazada por la DIAN: " + extraerErroresSiigo(invoiceInfo));
                }
                dianEntity = dianRepository.save(dianEntity);
            }
        } catch (Exception e) {
            log.error(">> [SIIGO DIAN] Error al sincronizar estado de factura {}: {}", factura.getNumeroFactura(), e.getMessage());
        }

        return mapToDto(dianEntity);
    }

    private String extraerErroresSiigo(SiigoInvoiceResponseDto response) {
        if (response == null) return "Sin detalle";
        StringBuilder sb = new StringBuilder();
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            response.getErrors().forEach(err -> sb.append(err.getMessage() != null ? err.getMessage() : err.getCode()).append("; "));
        }
        if (response.getStamp() != null && response.getStamp().getErrors() != null) {
            response.getStamp().getErrors().forEach(err -> sb.append(err.getMessage() != null ? err.getMessage() : err.getCode()).append("; "));
        }
        return sb.length() > 0 ? sb.toString() : "Rechazo de validación fiscal";
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
                BigDecimal basePrice = it.getValorUnitario();
                if (!esReinspeccion && itemTaxes != null && !itemTaxes.isEmpty() && basePrice != null && basePrice.compareTo(BigDecimal.ZERO) > 0) {
                    basePrice = basePrice.divide(new BigDecimal("1.19"), 2, RoundingMode.HALF_UP);
                }
                items.add(SiigoInvoiceRequestDto.InvoiceItemDto.builder()
                        .code(mapeo.getCodigoProductoSiigo())
                        .description(it.getDescripcion())
                        .quantity(it.getCantidad())
                        .price(basePrice)
                        .discount(BigDecimal.ZERO)
                        .taxes(itemTaxes)
                        .build());
            }
        } else {
            items.add(SiigoInvoiceRequestDto.InvoiceItemDto.builder()
                    .code(mapeo.getCodigoProductoSiigo())
                    .description(mapeo.getDescripcionSiigo())
                    .quantity(1)
                    .price(factura.getSubtotal() != null ? factura.getSubtotal() : BigDecimal.ZERO)
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

    public void despacharCorreoFacturaDian(FacturaEntity factura, FacturaElectronicaDianEntity dianEntity) {
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

    public void despacharWhatsAppFacturaDian(FacturaEntity factura, FacturaElectronicaDianEntity dianEntity) {
        try {
            ClienteEntity pagador = factura.getClienteFactura();
            if (pagador != null && pagador.getCelular() != null && !pagador.getCelular().isBlank()) {
                String placa = factura.getOrdenIngreso() != null && factura.getOrdenIngreso().getVehiculo() != null
                        ? factura.getOrdenIngreso().getVehiculo().getPlaca()
                        : "N/A";
                String numFacturaOficial = dianEntity.getNumeroFacturaSiigo() != null
                        ? dianEntity.getNumeroFacturaSiigo()
                        : factura.getNumeroFactura();

                String mensajeTexto = String.format(
                        "¡Hola %s! En CDA San Pedro confirmamos la emisión de tu Factura Electrónica %s para el vehículo %s por valor de $%s. Puedes consultar tu factura oficial DIAN en: %s",
                        pagador.getNombresRazonSocial(),
                        numFacturaOficial,
                        placa,
                        factura.getTotal().toPlainString(),
                        dianEntity.getPdfSiigoUrl() != null ? dianEntity.getPdfSiigoUrl() : "https://cdasanpedro.com"
                );

                notificacionService.encolarNotificacion(
                        pagador,
                        "FACTURA_EMISION_DIAN",
                        "WHATSAPP",
                        pagador.getCelular(),
                        "Factura Electrónica " + numFacturaOficial + " - CDA San Pedro",
                        String.format("{\"mensaje\": \"%s\", \"numeroFactura\": \"%s\", \"placa\": \"%s\", \"total\": \"%s\", \"url\": \"%s\"}",
                                mensajeTexto, numFacturaOficial, placa, factura.getTotal().toPlainString(),
                                dianEntity.getPdfSiigoUrl() != null ? dianEntity.getPdfSiigoUrl() : "")
                );
                notificacionService.despacharColaPendiente();
            }
        } catch (Exception e) {
            log.warn(">> [SIIGO DIAN] No se pudo encolar WhatsApp: {}", e.getMessage());
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
