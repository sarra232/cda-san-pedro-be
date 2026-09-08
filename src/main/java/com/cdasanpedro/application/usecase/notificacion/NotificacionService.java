package com.cdasanpedro.application.usecase.notificacion;

import com.cdasanpedro.application.dto.notificacion.NotificacionResponseDto;
import com.cdasanpedro.core.exception.ResourceNotFoundException;
import com.cdasanpedro.core.gateway.NotificationGateway;
import com.cdasanpedro.infrastructure.pdf.PdfGeneratorService;
import com.cdasanpedro.infrastructure.persistence.entity.ClienteEntity;
import com.cdasanpedro.infrastructure.persistence.entity.FacturaEntity;
import com.cdasanpedro.infrastructure.persistence.entity.ItemFacturaEntity;
import com.cdasanpedro.infrastructure.persistence.entity.NotificacionEntity;
import com.cdasanpedro.infrastructure.persistence.entity.OrdenIngresoEntity;
import com.cdasanpedro.infrastructure.persistence.entity.VehiculoEntity;
import com.cdasanpedro.infrastructure.persistence.repository.ClienteRepository;
import com.cdasanpedro.infrastructure.persistence.repository.FacturaRepository;
import com.cdasanpedro.infrastructure.persistence.repository.NotificacionRepository;
import com.cdasanpedro.infrastructure.persistence.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ClienteRepository clienteRepository;
    private final FacturaRepository facturaRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final NotificationGateway notificationGateway;

    /**
     * Tarea programada diaria a las 08:00 AM (y ejecutable manualmente).
     * Realiza el barrido de vencimientos de SOAT, RTM y cumpleaños.
     */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void procesarBarridoDiario() {
        log.info("Iniciando barrido programado de notificaciones de SOAT, RTM y Cumpleaños...");
        LocalDate hoy = LocalDate.now();

        // 1. Barrido de SOAT y RTM próximos a vencer o vencidos
        List<VehiculoEntity> vehiculos = vehiculoRepository.findAll();
        for (VehiculoEntity veh : vehiculos) {
            ClienteEntity prop = veh.getPropietario();
            if (prop == null || (prop.getCelular() == null && prop.getEmail() == null)) {
                continue;
            }

            // SOAT
            if (veh.getFechaVencimientoSoat() != null) {
                long diasSoat = java.time.temporal.ChronoUnit.DAYS.between(hoy, veh.getFechaVencimientoSoat());
                if (diasSoat == 15 || diasSoat == 7 || diasSoat == 0) {
                    encolarNotificacion(
                            prop,
                            "SOAT_VENCIMIENTO",
                            "WHATSAPP",
                            prop.getCelular(),
                            "Recordatorio de Vencimiento de SOAT",
                            String.format("{\"mensaje\": \"Hola %s, tu SOAT para el vehículo %s vence en %d días (%s). ¡Renueva a tiempo en CDA San Pedro!\", \"placa\": \"%s\"}",
                                    prop.getNombresRazonSocial(), veh.getPlaca(), diasSoat, veh.getFechaVencimientoSoat(), veh.getPlaca())
                    );
                }
            }

            // RTM (Tecnomecánica)
            if (veh.getFechaVencimientoRtm() != null) {
                long diasRtm = java.time.temporal.ChronoUnit.DAYS.between(hoy, veh.getFechaVencimientoRtm());
                if (diasRtm == 15 || diasRtm == 7 || diasRtm == 0) {
                    encolarNotificacion(
                            prop,
                            "RTM_VENCIMIENTO",
                            "WHATSAPP",
                            prop.getCelular(),
                            "Recordatorio de Tecnomecánica RTM",
                            String.format("{\"mensaje\": \"Estimado %s, la revisión Tecnomecánica de tu auto %s (%s %s) vence en %d días (%s). Evita multas y visítanos en CDA San Pedro.\", \"placa\": \"%s\"}",
                                    prop.getNombresRazonSocial(), veh.getPlaca(), veh.getMarca(), veh.getLinea(), diasRtm, veh.getFechaVencimientoRtm(), veh.getPlaca())
                    );
                }
            }
        }

        // 2. Barrido de Cumpleaños de Clientes
        List<ClienteEntity> clientes = clienteRepository.findAll();
        for (ClienteEntity c : clientes) {
            if (c.getFechaNacimiento() != null && c.getCelular() != null) {
                if (c.getFechaNacimiento().getMonth() == hoy.getMonth() && c.getFechaNacimiento().getDayOfMonth() == hoy.getDayOfMonth()) {
                    encolarNotificacion(
                            c,
                            "CUMPLEANOS",
                            "WHATSAPP",
                            c.getCelular(),
                            "¡Feliz Cumpleaños de parte de CDA San Pedro!",
                            String.format("{\"mensaje\": \"¡Feliz Cumpleaños %s! 🎂 En CDA San Pedro celebramos contigo y te deseamos un gran día lleno de éxitos.\", \"clienteId\": \"%s\"}",
                                    c.getNombresRazonSocial(), c.getId())
                    );
                }
            }
        }

        // 3. Despacho de la cola de notificaciones pendientes
        despacharColaPendiente();
    }

    @Transactional
    public void encolarNotificacion(ClienteEntity cliente, String tipo, String canal, String destinatario, String asunto, String jsonPayload) {
        NotificacionEntity notif = NotificacionEntity.builder()
                .cliente(cliente)
                .tipo(tipo)
                .canal(canal)
                .destinatario(destinatario != null ? destinatario : "")
                .asunto(asunto)
                .cuerpoPayload(jsonPayload)
                .estado("PENDIENTE")
                .intentos(0)
                .fechaProgramada(OffsetDateTime.now())
                .build();

        notificacionRepository.save(notif);
    }

    @Transactional
    public void despacharColaPendiente() {
        List<NotificacionEntity> pendientes = notificacionRepository.findPendientesParaEnvio(OffsetDateTime.now());
        for (NotificacionEntity n : pendientes) {
            despacharNotificacion(n);
        }
    }

    private void despacharNotificacion(NotificacionEntity n) {
        try {
            if ("EMAIL".equalsIgnoreCase(n.getCanal())) {
                notificationGateway.sendEmail(n.getDestinatario(), n.getAsunto(), n.getCuerpoPayload(), null, null);
            } else {
                notificationGateway.sendWhatsAppMessage(n.getDestinatario(), n.getCuerpoPayload(), new HashMap<>());
            }
            n.setEstado("ENVIADO");
            n.setFechaEnviado(OffsetDateTime.now());
            n.setIntentos(n.getIntentos() + 1);
        } catch (Exception e) {
            log.error("Error al despachar notificación ID {}: {}", n.getId(), e.getMessage());
            n.setIntentos(n.getIntentos() + 1);
            if (n.getIntentos() >= 3) {
                n.setEstado("FALLIDO");
            }
        }
        notificacionRepository.save(n);
    }

    @Transactional
    public NotificacionResponseDto reintentar(UUID id) {
        NotificacionEntity n = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada con ID: " + id));

        despacharNotificacion(n);
        return toDto(n);
    }

    @Transactional
    public void enviarCorreoPrueba(String destinatario, String mensaje) {
        log.info("Despachando correo de prueba a: {}", destinatario);
        String cuerpo = (mensaje != null && !mensaje.isBlank()) 
                ? mensaje 
                : "Este es un mensaje de prueba emitido desde el Sistema Oficial de CDA San Pedro para validar la conectividad SMTP de Gmail.";
        notificationGateway.sendEmail(destinatario, "Prueba de Conectividad - CDA San Pedro", cuerpo, null, null);
    }

    @Transactional
    public String enviarPlantillaReal(com.cdasanpedro.application.dto.notificacion.SendRealEmailRequestDto req) {
        String to = req.getDestinatario().trim();
        String tipo = req.getTipoPlantilla() != null ? req.getTipoPlantilla().toUpperCase().trim() : "GENERAL";
        String htmlBody;
        String subject;

        log.info("Despachando plantilla real [{}] a destinatario: {}", tipo, to);

        switch (tipo) {
            case "RECORDATORIO_RTM" -> {
                int dias = req.getDiasRestantes() != null ? req.getDiasRestantes() : 15;
                LocalDate fechaVenc = req.getFechaVencimiento() != null ? req.getFechaVencimiento() : LocalDate.now().plusDays(dias);
                subject = "🚨 Recordatorio: Revisión Técnico-Mecánica próxima a vencer - Placa " + (req.getPlaca() != null ? req.getPlaca().toUpperCase() : "VEHÍCULO");
                htmlBody = com.cdasanpedro.infrastructure.notification.EmailTemplateBuilder.buildRecordatorioRtm(
                        req.getNombreCliente(), req.getPlaca(), req.getCategoriaVehiculo(), fechaVenc, dias
                );
            }
            case "COMPROBANTE_PAGO" -> {
                String numFactura = req.getNumeroFactura() != null ? req.getNumeroFactura() : "FAC-00128";
                subject = "🧾 Comprobante de Pago y Facturación Electrónica DIAN " + numFactura + " - CDA San Pedro";
                
                byte[] pdfBytes = null;
                String attachmentName = "Factura_DIAN_" + numFactura.replace(" ", "_") + ".pdf";

                // 1. Buscar si existe en BD
                Optional<FacturaEntity> facturaOpt = facturaRepository.findByNumeroFactura(numFactura);
                if (facturaOpt.isPresent()) {
                    pdfBytes = pdfGeneratorService.generarFacturaPdf(facturaOpt.get());
                } else {
                    // 2. Generar Factura en memoria para adjuntar al correo
                    BigDecimal total = req.getTotal() != null ? req.getTotal() : new BigDecimal("248000");
                    BigDecimal subtotal = total.divide(new BigDecimal("1.19"), 2, RoundingMode.HALF_UP);
                    BigDecimal iva = total.subtract(subtotal);

                    ClienteEntity dummyCliente = ClienteEntity.builder()
                            .nombresRazonSocial(req.getNombreCliente() != null && !req.getNombreCliente().isBlank() ? req.getNombreCliente() : "Cliente CDA San Pedro")
                            .numeroDocumento("0000000000")
                            .tipoDocumento(com.cdasanpedro.core.model.enums.TipoDocumento.CC)
                            .email(to)
                            .celular("0000000000")
                            .build();

                    VehiculoEntity dummyVehiculo = VehiculoEntity.builder()
                            .placa(req.getPlaca() != null && !req.getPlaca().isBlank() ? req.getPlaca().toUpperCase() : "CDA000")
                            .marca("VEHICULO")
                            .linea("PARTICULAR")
                            .modelo(2025)
                            .categoria(com.cdasanpedro.core.model.enums.CategoriaVehiculo.LIVIANO)
                            .build();

                    OrdenIngresoEntity dummyOrden = OrdenIngresoEntity.builder()
                            .consecutivo(1000L)
                            .vehiculo(dummyVehiculo)
                            .build();

                    FacturaEntity dummyFactura = FacturaEntity.builder()
                            .numeroFactura(numFactura)
                            .fechaEmision(OffsetDateTime.now())
                            .subtotal(subtotal)
                            .iva(iva)
                            .total(total)
                            .metodoPago(req.getMetodoPago() != null && !req.getMetodoPago().isBlank() 
                                    ? (req.getMetodoPago().equalsIgnoreCase("TRANSFERENCIA") ? com.cdasanpedro.core.model.enums.MetodoPago.TRANSFERENCIA : com.cdasanpedro.core.model.enums.MetodoPago.EFECTIVO)
                                    : com.cdasanpedro.core.model.enums.MetodoPago.EFECTIVO)
                            .estado(com.cdasanpedro.core.model.enums.EstadoFactura.PAGADA)
                            .clienteFactura(dummyCliente)
                            .ordenIngreso(dummyOrden)
                            .build();

                    dummyFactura.addItem(ItemFacturaEntity.builder()
                            .descripcion("Revisión Técnico-Mecánica y Emisiones Contaminantes (LIVIANO - " + dummyVehiculo.getPlaca() + ")")
                            .cantidad(1)
                            .valorUnitario(total)
                            .totalItem(total)
                            .build());

                    pdfBytes = pdfGeneratorService.generarFacturaPdf(dummyFactura);
                }

                String cufeParam = (req.getCufe() != null && !req.getCufe().isBlank()) ? req.getCufe().trim() : null;
                String siigoUrlParam = (req.getPdfUrl() != null && !req.getPdfUrl().isBlank()) ? req.getPdfUrl().trim() : null;

                htmlBody = com.cdasanpedro.infrastructure.notification.EmailTemplateBuilder.buildComprobantePago(
                        req.getNombreCliente(), req.getPlaca(), numFactura, req.getTotal(), req.getMetodoPago(), cufeParam, siigoUrlParam
                );

                notificationGateway.sendEmail(to, subject, htmlBody, pdfBytes, attachmentName);
                return "Plantilla COMPROBANTE_PAGO con PDF adjunto (" + attachmentName + ") despachada exitosamente a " + to;
            }
            case "CUMPLEANOS" -> {
                subject = "🎂 ¡Feliz Cumpleaños " + (req.getNombreCliente() != null ? req.getNombreCliente() : "") + "! Te desea CDA San Pedro 🎉";
                htmlBody = com.cdasanpedro.infrastructure.notification.EmailTemplateBuilder.buildCumpleanos(
                        req.getNombreCliente(), req.getCuponOBeneficio()
                );
            }
            case "INSPECCION_FINALIZADA" -> {
                subject = "✅ Inspección Finalizada: Tu vehículo " + (req.getPlaca() != null ? req.getPlaca().toUpperCase() : "") + " ha sido APROBADO - CDA San Pedro";
                htmlBody = com.cdasanpedro.infrastructure.notification.EmailTemplateBuilder.buildInspeccionAprobada(
                        req.getNombreCliente(), req.getPlaca(), req.getCertificadoRurt(), LocalDate.now().toString()
                );
            }
            default -> {
                subject = "Notificación Oficial - CDA San Pedro";
                htmlBody = req.getMensaje() != null ? req.getMensaje() : "Mensaje de prueba oficial desde CDA San Pedro.";
            }
        }

        notificationGateway.sendEmail(to, subject, htmlBody, null, null);
        return "Plantilla " + tipo + " despachada exitosamente a " + to;
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponseDto> listarNotificaciones() {
        return notificacionRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public NotificacionResponseDto toDto(NotificacionEntity entity) {
        return NotificacionResponseDto.builder()
                .id(entity.getId())
                .tipo(entity.getTipo())
                .canal(entity.getCanal())
                .destinatario(entity.getDestinatario())
                .asunto(entity.getAsunto())
                .cuerpoPayload(entity.getCuerpoPayload())
                .estado(entity.getEstado())
                .intentos(entity.getIntentos())
                .fechaProgramada(entity.getFechaProgramada())
                .fechaEnviado(entity.getFechaEnviado())
                .clienteNombre(entity.getCliente() != null ? entity.getCliente().getNombresRazonSocial() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
