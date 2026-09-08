package com.cdasanpedro.application.usecase.factura;

import com.cdasanpedro.application.dto.cliente.ClienteResponseDto;
import com.cdasanpedro.application.dto.factura.FacturaRequestDto;
import com.cdasanpedro.application.dto.factura.FacturaResponseDto;
import com.cdasanpedro.application.dto.factura.ItemFacturaRequestDto;
import com.cdasanpedro.application.dto.factura.ItemFacturaResponseDto;
import com.cdasanpedro.application.usecase.cliente.ClienteService;
import com.cdasanpedro.application.usecase.ingreso.OrdenIngresoService;
import com.cdasanpedro.core.exception.BusinessException;
import com.cdasanpedro.core.exception.ResourceNotFoundException;
import com.cdasanpedro.core.model.enums.CategoriaVehiculo;
import com.cdasanpedro.core.model.enums.EstadoFactura;
import com.cdasanpedro.core.model.enums.EstadoOrden;
import com.cdasanpedro.core.gateway.NotificationGateway;
import com.cdasanpedro.infrastructure.notification.EmailTemplateBuilder;
import com.cdasanpedro.infrastructure.pdf.PdfGeneratorService;
import com.cdasanpedro.infrastructure.persistence.entity.*;
import com.cdasanpedro.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final OrdenIngresoRepository ordenIngresoRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteService clienteService;
    private final OrdenIngresoService ordenIngresoService;
    private final PdfGeneratorService pdfGeneratorService;
    private final NotificationGateway notificationGateway;
    private final com.cdasanpedro.application.usecase.notificacion.NotificacionService notificacionService;
    private final com.cdasanpedro.application.usecase.tarifa.TarifaService tarifaService;

    private static final BigDecimal FACTOR_IVA = new BigDecimal("1.19");

    @Transactional
    public FacturaResponseDto emitirFactura(FacturaRequestDto request, UUID usuarioId) {
        // 1. Obtener la orden de ingreso
        OrdenIngresoEntity orden = ordenIngresoRepository.findById(request.getOrdenIngresoId())
                .orElseThrow(() -> new ResourceNotFoundException("Orden de ingreso no encontrada con ID: " + request.getOrdenIngresoId()));

        if (facturaRepository.findByOrdenIngresoId(orden.getId()).isPresent()) {
            throw new BusinessException("La orden de ingreso #" + orden.getConsecutivo() + " ya tiene una factura emitida.");
        }

        // 2. Obtener usuario cajero
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario cajero no encontrado con ID: " + usuarioId));

        // 3. Resolver el cliente pagador de la factura
        ClienteEntity clientePagador = resolverClientePagador(request, orden);

        // 4. Calcular Valores Financieros e Ítems
        BigDecimal totalBruto = BigDecimal.ZERO;
        List<ItemFacturaEntity> itemsEntity = new ArrayList<>();

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (ItemFacturaRequestDto itDto : request.getItems()) {
                BigDecimal itemTotal = itDto.getValorUnitario().multiply(new BigDecimal(itDto.getCantidad()));
                totalBruto = totalBruto.add(itemTotal);
                itemsEntity.add(ItemFacturaEntity.builder()
                        .descripcion(itDto.getDescripcion())
                        .cantidad(itDto.getCantidad())
                        .valorUnitario(itDto.getValorUnitario())
                        .totalItem(itemTotal)
                        .build());
            }
        } else {
            boolean esReinspeccionGratuita = Boolean.TRUE.equals(orden.getEsReinspeccion()) 
                    || "REINSPECCION_GRATUITA".equalsIgnoreCase(orden.getTipoServicio());
            
            BigDecimal tarifaTotal = esReinspeccionGratuita 
                    ? BigDecimal.ZERO 
                    : getTarifaPorCategoria(orden.getVehiculo().getCategoria());
            
            String descripcion = esReinspeccionGratuita
                    ? "2da Revisión / Reinspección RTM Gratuita (15 Días) - " + orden.getVehiculo().getPlaca()
                    : "Revisión Técnico-Mecánica y Emisiones Contaminantes (" + orden.getVehiculo().getCategoria() + " - " + orden.getVehiculo().getPlaca() + ")";

            totalBruto = tarifaTotal;
            itemsEntity.add(ItemFacturaEntity.builder()
                    .descripcion(descripcion)
                    .cantidad(1)
                    .valorUnitario(tarifaTotal)
                    .totalItem(tarifaTotal)
                    .build());
        }

        // Desglose de IVA 19%
        BigDecimal subtotal = totalBruto.divide(FACTOR_IVA, 2, RoundingMode.HALF_UP);
        BigDecimal iva = totalBruto.subtract(subtotal);
        BigDecimal total = totalBruto.setScale(2, RoundingMode.HALF_UP);

        // Generar número de factura (FAC- + timestamp/consecutivo)
        long count = facturaRepository.count() + 1;
        String numeroFactura = String.format("FAC-%05d", count);

        // 5. Crear la Factura
        FacturaEntity factura = FacturaEntity.builder()
                .numeroFactura(numeroFactura)
                .fechaEmision(OffsetDateTime.now())
                .subtotal(subtotal)
                .iva(iva)
                .total(total)
                .metodoPago(request.getMetodoPago())
                .estado(EstadoFactura.PAGADA)
                .ordenIngreso(orden)
                .clienteFactura(clientePagador)
                .usuario(usuario)
                .build();

        for (ItemFacturaEntity item : itemsEntity) {
            factura.addItem(item);
        }

        FacturaEntity guardada = facturaRepository.save(factura);

        // 6. Actualizar estado de la orden a FACTURADO
        orden.setEstado(EstadoOrden.FACTURADO);
        ordenIngresoRepository.save(orden);

        // 7. Despacho automático de la factura al cliente/pagador (WhatsApp / Correo)
        despacharNotificacionFactura(guardada);

        return toDto(guardada);
    }

    @Transactional
    public void enviarFacturaCliente(UUID facturaId) {
        FacturaEntity factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con ID: " + facturaId));
        despacharNotificacionFactura(factura);
    }

    private void despacharNotificacionFactura(FacturaEntity factura) {
        try {
            ClienteEntity pagador = factura.getClienteFactura();
            if (pagador != null) {
                String placa = factura.getOrdenIngreso() != null && factura.getOrdenIngreso().getVehiculo() != null
                        ? factura.getOrdenIngreso().getVehiculo().getPlaca()
                        : "N/A";

                String mensajeTexto = String.format(
                        "¡Hola %s! En CDA San Pedro agradecemos tu visita. Tu factura de venta %s para el vehículo %s por valor de $%s (%s) ha sido emitida exitosamente. ¡Seguridad y precisión para tu vehículo!",
                        pagador.getNombresRazonSocial(),
                        factura.getNumeroFactura(),
                        placa,
                        factura.getTotal().toPlainString(),
                        factura.getMetodoPago()
                );

                // 1. WhatsApp
                if (pagador.getCelular() != null && !pagador.getCelular().isBlank()) {
                    notificacionService.encolarNotificacion(
                            pagador,
                            "FACTURA_EMISION",
                            "WHATSAPP",
                            pagador.getCelular(),
                            "Factura de Venta " + factura.getNumeroFactura() + " - CDA San Pedro",
                            String.format("{\"mensaje\": \"%s\", \"numeroFactura\": \"%s\", \"placa\": \"%s\", \"total\": \"%s\"}",
                                    mensajeTexto, factura.getNumeroFactura(), placa, factura.getTotal().toPlainString())
                    );
                    notificacionService.despacharColaPendiente();
                }

                // 2. Email Oficial con PDF Adjunto Legal
                if (pagador.getEmail() != null && !pagador.getEmail().isBlank()) {
                    try {
                        byte[] pdfBytes = pdfGeneratorService.generarFacturaPdf(factura);
                        String htmlBody = EmailTemplateBuilder.buildComprobantePago(
                                pagador.getNombresRazonSocial(),
                                placa,
                                factura.getNumeroFactura(),
                                factura.getTotal(),
                                factura.getMetodoPago() != null ? factura.getMetodoPago().name() : "EFECTIVO",
                                null,
                                null
                        );

                        String attachmentName = "Factura_" + factura.getNumeroFactura().replace(" ", "_") + ".pdf";
                        notificationGateway.sendEmail(
                                pagador.getEmail().trim(),
                                "🧾 Comprobante Oficial Factura " + factura.getNumeroFactura() + " - CDA San Pedro (PDF Adjunto)",
                                htmlBody,
                                pdfBytes,
                                attachmentName
                        );
                        log.info(">> [FacturaService] Correo con PDF de factura {} enviado a {}", factura.getNumeroFactura(), pagador.getEmail());
                    } catch (Exception exMail) {
                        log.error(">> [FacturaService] Error enviando correo con PDF de factura {}: {}", factura.getNumeroFactura(), exMail.getMessage(), exMail);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al despachar notificación de factura {}: {}", factura.getNumeroFactura(), e.getMessage());
        }
    }

    private ClienteEntity resolverClientePagador(FacturaRequestDto request, OrdenIngresoEntity orden) {
        String tipo = request.getPagadorTipo() != null ? request.getPagadorTipo().toUpperCase() : "PROPIETARIO";

        if ("CONDUCTOR".equals(tipo)) {
            if (request.getClienteFacturaData() != null && request.getClienteFacturaData().getNumeroDocumento() != null && !request.getClienteFacturaData().getNumeroDocumento().isBlank()) {
                ClienteResponseDto dto = clienteService.registrarOActualizar(request.getClienteFacturaData());
                ClienteEntity conductor = clienteRepository.findById(dto.getId()).orElseThrow();
                orden.setConductor(conductor);
                ordenIngresoRepository.save(orden);
                return conductor;
            }
            if (orden.getConductor() != null) {
                return orden.getConductor();
            }
            if (orden.getVehiculo().getPropietario() != null) {
                return orden.getVehiculo().getPropietario();
            }
        } else if ("TERCERO".equals(tipo)) {
            if (request.getClienteFacturaId() != null) {
                return clienteRepository.findById(request.getClienteFacturaId())
                        .orElseThrow(() -> new ResourceNotFoundException("Tercero pagador no encontrado con ID: " + request.getClienteFacturaId()));
            } else if (request.getClienteFacturaData() != null) {
                ClienteResponseDto dto = clienteService.registrarOActualizar(request.getClienteFacturaData());
                return clienteRepository.findById(dto.getId()).orElseThrow();
            }
        }

        // Por defecto: Propietario del vehículo
        if (request.getClienteFacturaData() != null && request.getClienteFacturaData().getNumeroDocumento() != null && !request.getClienteFacturaData().getNumeroDocumento().isBlank()) {
            ClienteResponseDto dto = clienteService.registrarOActualizar(request.getClienteFacturaData());
            ClienteEntity propietario = clienteRepository.findById(dto.getId()).orElseThrow();
            orden.getVehiculo().setPropietario(propietario);
            vehiculoRepository.save(orden.getVehiculo());
            return propietario;
        }

        if (orden.getVehiculo().getPropietario() != null) {
            return orden.getVehiculo().getPropietario();
        } else if (orden.getConductor() != null) {
            return orden.getConductor();
        } else {
            throw new BusinessException("No se encontró ningún cliente asociado al vehículo o conductor para facturar. Por favor ingrese los datos del pagador.");
        }
    }

    private BigDecimal getTarifaPorCategoria(CategoriaVehiculo categoria) {
        return tarifaService.obtenerPrecioPorCategoria(categoria);
    }

    @Transactional(readOnly = true)
    public byte[] generarPdf(UUID facturaId) {
        FacturaEntity factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con ID: " + facturaId));
        return pdfGeneratorService.generarFacturaPdf(factura);
    }

    @Transactional(readOnly = true)
    public FacturaResponseDto obtenerPorId(UUID id) {
        FacturaEntity factura = facturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con ID: " + id));
        return toDto(factura);
    }

    @Transactional(readOnly = true)
    public List<FacturaResponseDto> listarFacturas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio != null && fechaFin != null) {
            OffsetDateTime start = fechaInicio.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime end = fechaFin.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
            return facturaRepository.findByFechaEmisionBetween(start, end)
                    .stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        return facturaRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public FacturaResponseDto toDto(FacturaEntity entity) {
        ClienteResponseDto clienteDto = clienteService.toDto(entity.getClienteFactura());
        var ordenDto = ordenIngresoService.toDto(entity.getOrdenIngreso());

        List<ItemFacturaResponseDto> itemsDto = entity.getItems() != null
                ? entity.getItems().stream().map(i -> ItemFacturaResponseDto.builder()
                        .id(i.getId())
                        .descripcion(i.getDescripcion())
                        .cantidad(i.getCantidad())
                        .valorUnitario(i.getValorUnitario())
                        .totalItem(i.getTotalItem())
                        .build()).collect(Collectors.toList())
                : new ArrayList<>();

        return FacturaResponseDto.builder()
                .id(entity.getId())
                .numeroFactura(entity.getNumeroFactura())
                .consecutivo(entity.getConsecutivo())
                .fechaEmision(entity.getFechaEmision())
                .subtotal(entity.getSubtotal())
                .iva(entity.getIva())
                .total(entity.getTotal())
                .metodoPago(entity.getMetodoPago())
                .estado(entity.getEstado())
                .clienteFactura(clienteDto)
                .ordenIngreso(ordenDto)
                .usuarioNombre(entity.getUsuario() != null ? entity.getUsuario().getNombresApellidos() : null)
                .items(itemsDto)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
