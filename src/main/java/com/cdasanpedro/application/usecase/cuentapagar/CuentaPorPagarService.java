package com.cdasanpedro.application.usecase.cuentapagar;

import com.cdasanpedro.application.dto.cuentapagar.*;
import com.cdasanpedro.core.model.enums.EstadoCuentaPagar;
import com.cdasanpedro.core.model.enums.PeriodicidadPago;
import com.cdasanpedro.core.model.enums.TipoObligacion;
import com.cdasanpedro.infrastructure.persistence.entity.CuentaPorPagarEntity;
import com.cdasanpedro.infrastructure.persistence.entity.PagoProveedorEntity;
import com.cdasanpedro.infrastructure.persistence.entity.TerceroEntity;
import com.cdasanpedro.infrastructure.persistence.entity.UsuarioEntity;
import com.cdasanpedro.infrastructure.persistence.repository.CuentaPorPagarRepository;
import com.cdasanpedro.infrastructure.persistence.repository.PagoProveedorRepository;
import com.cdasanpedro.infrastructure.persistence.repository.TerceroRepository;
import com.cdasanpedro.infrastructure.persistence.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaPorPagarService {

    private final CuentaPorPagarRepository cuentaPorPagarRepository;
    private final PagoProveedorRepository pagoProveedorRepository;
    private final TerceroRepository terceroRepository;
    private final UsuarioRepository usuarioRepository;
    private final com.cdasanpedro.infrastructure.persistence.repository.ConfiguracionSistemaRepository configuracionRepository;
    private final com.cdasanpedro.application.usecase.notificacion.NotificacionService notificacionService;

    @Value("${app.notificaciones.tesoreria.emails:administracion@cdasanpedro.com,gerencia@cdasanpedro.com}")
    private String defaultEmailsConfig;

    @Value("${app.notificaciones.tesoreria.telefonos:3000000000}")
    private String defaultTelefonosConfig;

    /**
     * Tarea programada automática: Se ejecuta todos los días a las 08:00 AM.
     * Evalúa las obligaciones vencidas y próximas a vencer y despacha
     * automáticamente las alertas por Email y SMS/WhatsApp a la gerencia y tesorería.
     */
    @Scheduled(cron = "${app.notificaciones.tesoreria.cron:0 0 8 * * ?}")
    @Transactional
    public void procesarAlertasAutomaticasDiarias() {
        log.info(">> [CRON TESORERÍA] Iniciando barrido automático de cuentas por pagar y facturas pendientes...");
        
        // Verificar si las alertas están activas en base de datos
        var confActivo = configuracionRepository.findByClave("NOTIFICACIONES_TESORERIA_ACTIVO");
        if (confActivo.isPresent() && "false".equalsIgnoreCase(confActivo.get().getValor())) {
            log.info(">> [CRON TESORERÍA] Las alertas automáticas están desactivadas por configuración del usuario.");
            return;
        }

        SemaforoVencimientosDto semaforo = obtenerSemaforoVencimientos();

        long totalAlertas = semaforo.getTotalVencidas() + semaforo.getTotalProximas();
        if (totalAlertas == 0) {
            log.info(">> [CRON TESORERÍA] Semáforo financiero al día: 0 facturas pendientes o próximas a vencer. No se requiere envío.");
            return;
        }

        log.warn(">> [CRON TESORERÍA] Se detectaron {} obligaciones pendientes ({} vencidas, {} próximas a vencer). Despachando notificación automática...",
                totalAlertas, semaforo.getTotalVencidas(), semaforo.getTotalProximas());

        List<String> emails = obtenerDestinatariosEmails(null);
        List<String> telefonos = obtenerDestinatariosTelefonos(null);

        notificacionService.notificarCuentasPorPagarPendientes(emails, telefonos, semaforo);
        log.info(">> [CRON TESORERÍA] Notificaciones automáticas enviadas exitosamente a {} correos y {} números.",
                emails.size(), telefonos.size());
    }

    @Transactional
    public void notificarPendientes(NotificarCuentasRequestDto request) {
        SemaforoVencimientosDto semaforo = obtenerSemaforoVencimientos();
        List<String> emails = obtenerDestinatariosEmails(request != null ? request.getEmails() : null);
        List<String> telefonos = obtenerDestinatariosTelefonos(request != null ? request.getTelefonos() : null);

        notificacionService.notificarCuentasPorPagarPendientes(emails, telefonos, semaforo);
    }

    @Transactional(readOnly = true)
    public ConfiguracionAlertasTesoreriaDto obtenerConfiguracionAlertas() {
        String emailsStr = configuracionRepository.findByClave("NOTIFICACIONES_TESORERIA_EMAILS")
                .map(com.cdasanpedro.infrastructure.persistence.entity.ConfiguracionSistemaEntity::getValor)
                .orElse(defaultEmailsConfig);

        String telStr = configuracionRepository.findByClave("NOTIFICACIONES_TESORERIA_TELEFONOS")
                .map(com.cdasanpedro.infrastructure.persistence.entity.ConfiguracionSistemaEntity::getValor)
                .orElse(defaultTelefonosConfig);

        boolean activo = configuracionRepository.findByClave("NOTIFICACIONES_TESORERIA_ACTIVO")
                .map(c -> !"false".equalsIgnoreCase(c.getValor()))
                .orElse(true);

        String hora = configuracionRepository.findByClave("NOTIFICACIONES_TESORERIA_HORA")
                .map(com.cdasanpedro.infrastructure.persistence.entity.ConfiguracionSistemaEntity::getValor)
                .orElse("08:00");

        List<String> emailsList = Arrays.stream(emailsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        List<String> telList = Arrays.stream(telStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        return ConfiguracionAlertasTesoreriaDto.builder()
                .emails(emailsList)
                .telefonos(telList)
                .activo(activo)
                .horaEnvio(hora)
                .cronExpression("0 0 8 * * ?")
                .build();
    }

    @Transactional
    public ConfiguracionAlertasTesoreriaDto guardarConfiguracionAlertas(ConfiguracionAlertasTesoreriaDto dto) {
        if (dto.getEmails() != null) {
            String emailsJoin = String.join(", ", dto.getEmails());
            guardarOActualizarConfig("NOTIFICACIONES_TESORERIA_EMAILS", emailsJoin, "Lista de correos para alertas de tesorería");
        }
        if (dto.getTelefonos() != null) {
            String telJoin = String.join(", ", dto.getTelefonos());
            guardarOActualizarConfig("NOTIFICACIONES_TESORERIA_TELEFONOS", telJoin, "Lista de celulares para alertas SMS/WhatsApp");
        }
        if (dto.getActivo() != null) {
            guardarOActualizarConfig("NOTIFICACIONES_TESORERIA_ACTIVO", String.valueOf(dto.getActivo()), "Estado activo del cron");
        }
        if (dto.getHoraEnvio() != null) {
            guardarOActualizarConfig("NOTIFICACIONES_TESORERIA_HORA", dto.getHoraEnvio(), "Hora de despacho diario");
        }
        return obtenerConfiguracionAlertas();
    }

    private void guardarOActualizarConfig(String clave, String valor, String desc) {
        var entidad = configuracionRepository.findByClave(clave)
                .orElse(com.cdasanpedro.infrastructure.persistence.entity.ConfiguracionSistemaEntity.builder()
                        .clave(clave)
                        .categoria("TESORERIA")
                        .descripcion(desc)
                        .build());
        entidad.setValor(valor);
        configuracionRepository.save(entidad);
    }

    private List<String> obtenerDestinatariosEmails(List<String> solicitados) {
        Set<String> set = new HashSet<>();
        if (solicitados != null && !solicitados.isEmpty()) {
            for (String e : solicitados) {
                if (e != null && !e.isBlank()) set.add(e.trim());
            }
        }
        if (set.isEmpty()) {
            String emailsStr = configuracionRepository.findByClave("NOTIFICACIONES_TESORERIA_EMAILS")
                    .map(com.cdasanpedro.infrastructure.persistence.entity.ConfiguracionSistemaEntity::getValor)
                    .orElse(defaultEmailsConfig);

            if (emailsStr != null) {
                Arrays.stream(emailsStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .forEach(set::add);
            }
        }
        return new ArrayList<>(set);
    }

    private List<String> obtenerDestinatariosTelefonos(List<String> solicitados) {
        Set<String> set = new HashSet<>();
        if (solicitados != null && !solicitados.isEmpty()) {
            for (String t : solicitados) {
                if (t != null && !t.isBlank()) set.add(t.trim());
            }
        }
        if (set.isEmpty()) {
            String telStr = configuracionRepository.findByClave("NOTIFICACIONES_TESORERIA_TELEFONOS")
                    .map(com.cdasanpedro.infrastructure.persistence.entity.ConfiguracionSistemaEntity::getValor)
                    .orElse(defaultTelefonosConfig);

            if (telStr != null) {
                Arrays.stream(telStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .forEach(set::add);
            }
        }
        return new ArrayList<>(set);
    }

    @Transactional(readOnly = true)
    public List<CuentaPorPagarResponseDto> listarTodas() {
        return cuentaPorPagarRepository.findAllOrderByVencimientoAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CuentaPorPagarResponseDto obtenerPorId(UUID id) {
        CuentaPorPagarEntity entity = cuentaPorPagarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta por pagar no encontrada con ID: " + id));
        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public SemaforoVencimientosDto obtenerSemaforoVencimientos() {
        List<CuentaPorPagarEntity> todas = cuentaPorPagarRepository.findAllOrderByVencimientoAsc();
        LocalDate hoy = LocalDate.now();

        long vencidasCount = 0;
        BigDecimal saldoVencido = BigDecimal.ZERO;

        long proximasCount = 0;
        BigDecimal saldoProximo = BigDecimal.ZERO;

        long alDiaCount = 0;
        BigDecimal saldoAlDia = BigDecimal.ZERO;

        List<CuentaPorPagarResponseDto> urgentes = new ArrayList<>();

        for (CuentaPorPagarEntity c : todas) {
            if (c.getEstado() == EstadoCuentaPagar.PAGADA || c.getEstado() == EstadoCuentaPagar.ANULADA) {
                continue;
            }

            long dias = ChronoUnit.DAYS.between(hoy, c.getFechaVencimiento());
            int aviso = c.getDiasAvisoAnticipado() != null ? c.getDiasAvisoAnticipado() : 5;

            if (dias <= 0) {
                vencidasCount++;
                saldoVencido = saldoVencido.add(c.getSaldoPendiente());
                urgentes.add(mapToResponse(c));
            } else if (dias <= aviso || dias <= 7) {
                proximasCount++;
                saldoProximo = saldoProximo.add(c.getSaldoPendiente());
                urgentes.add(mapToResponse(c));
            } else {
                alDiaCount++;
                saldoAlDia = saldoAlDia.add(c.getSaldoPendiente());
            }
        }

        return SemaforoVencimientosDto.builder()
                .totalVencidas(vencidasCount)
                .saldoVencido(saldoVencido)
                .totalProximas(proximasCount)
                .saldoProximo(saldoProximo)
                .totalAlDia(alDiaCount)
                .saldoAlDia(saldoAlDia)
                .cuentasUrgentes(urgentes)
                .build();
    }

    @Transactional
    public CuentaPorPagarResponseDto crear(CuentaPorPagarRequestDto request) {
        TerceroEntity acreedor = terceroRepository.findById(request.getAcreedorTerceroId())
                .orElseThrow(() -> new IllegalArgumentException("Acreedor / Proveedor no encontrado"));

        CuentaPorPagarEntity entity = CuentaPorPagarEntity.builder()
                .acreedor(acreedor)
                .numeroReferencia(request.getNumeroReferencia())
                .concepto(request.getConcepto().trim())
                .montoTotal(request.getMontoTotal())
                .saldoPendiente(request.getMontoTotal())
                .tipoObligacion(request.getTipoObligacion() != null ? request.getTipoObligacion() : TipoObligacion.FACTURA_PROVEEDOR)
                .periodicidad(request.getPeriodicidad() != null ? request.getPeriodicidad() : PeriodicidadPago.PAGO_UNICO)
                .fechaEmision(request.getFechaEmision() != null ? request.getFechaEmision() : LocalDate.now())
                .fechaVencimiento(request.getFechaVencimiento())
                .diasAvisoAnticipado(request.getDiasAvisoAnticipado() != null ? request.getDiasAvisoAnticipado() : 5)
                .estado(EstadoCuentaPagar.PENDIENTE)
                .observaciones(request.getObservaciones())
                .metadata(request.getMetadata() != null ? request.getMetadata() : "{}")
                .pagos(new ArrayList<>())
                .build();

        return mapToResponse(cuentaPorPagarRepository.save(entity));
    }

    @Transactional
    public CuentaPorPagarResponseDto actualizar(UUID id, CuentaPorPagarRequestDto request) {
        CuentaPorPagarEntity entity = cuentaPorPagarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta por pagar no encontrada con ID: " + id));

        if (request.getAcreedorTerceroId() != null && !request.getAcreedorTerceroId().equals(entity.getAcreedor().getId())) {
            TerceroEntity acreedor = terceroRepository.findById(request.getAcreedorTerceroId())
                    .orElseThrow(() -> new IllegalArgumentException("Acreedor / Proveedor no encontrado"));
            entity.setAcreedor(acreedor);
        }

        if (request.getConcepto() != null) entity.setConcepto(request.getConcepto().trim());
        if (request.getNumeroReferencia() != null) entity.setNumeroReferencia(request.getNumeroReferencia().trim());

        if (request.getMontoTotal() != null) {
            BigDecimal totalPagado = entity.getPagos() != null
                    ? entity.getPagos().stream().map(PagoProveedorEntity::getMontoPagado).reduce(BigDecimal.ZERO, BigDecimal::add)
                    : BigDecimal.ZERO;
            
            BigDecimal nuevoSaldo = request.getMontoTotal().subtract(totalPagado);
            entity.setMontoTotal(request.getMontoTotal());
            entity.setSaldoPendiente(nuevoSaldo.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : nuevoSaldo);

            if (entity.getSaldoPendiente().compareTo(BigDecimal.ZERO) == 0) {
                entity.setEstado(EstadoCuentaPagar.PAGADA);
            } else if (totalPagado.compareTo(BigDecimal.ZERO) > 0) {
                entity.setEstado(EstadoCuentaPagar.PAGADA_PARCIAL);
            } else {
                entity.setEstado(EstadoCuentaPagar.PENDIENTE);
            }
        }

        if (request.getTipoObligacion() != null) entity.setTipoObligacion(request.getTipoObligacion());
        if (request.getPeriodicidad() != null) entity.setPeriodicidad(request.getPeriodicidad());
        if (request.getFechaEmision() != null) entity.setFechaEmision(request.getFechaEmision());
        if (request.getFechaVencimiento() != null) entity.setFechaVencimiento(request.getFechaVencimiento());
        if (request.getDiasAvisoAnticipado() != null) entity.setDiasAvisoAnticipado(request.getDiasAvisoAnticipado());
        if (request.getObservaciones() != null) entity.setObservaciones(request.getObservaciones());

        return mapToResponse(cuentaPorPagarRepository.save(entity));
    }

    @Transactional
    public void eliminar(UUID id) {
        CuentaPorPagarEntity entity = cuentaPorPagarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta por pagar no encontrada con ID: " + id));
        entity.setEstado(EstadoCuentaPagar.ANULADA);
        cuentaPorPagarRepository.save(entity);
    }

    @Transactional
    public CuentaPorPagarResponseDto registrarPago(UUID cuentaId, PagoProveedorRequestDto request, UUID usuarioId) {
        CuentaPorPagarEntity cuenta = cuentaPorPagarRepository.findById(cuentaId)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta por pagar no encontrada con ID: " + cuentaId));

        if (cuenta.getEstado() == EstadoCuentaPagar.PAGADA) {
            throw new IllegalStateException("Esta cuenta por pagar ya se encuentra totalmente cancelada");
        }

        UsuarioEntity usuario = usuarioRepository.findById(usuarioId != null ? usuarioId : request.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado para registrar el pago"));

        BigDecimal nuevoSaldo = cuenta.getSaldoPendiente().subtract(request.getMontoPagado());
        if (nuevoSaldo.compareTo(BigDecimal.ZERO) <= 0) {
            cuenta.setSaldoPendiente(BigDecimal.ZERO);
            cuenta.setEstado(EstadoCuentaPagar.PAGADA);
        } else {
            cuenta.setSaldoPendiente(nuevoSaldo);
            cuenta.setEstado(EstadoCuentaPagar.PAGADA_PARCIAL);
        }

        PagoProveedorEntity pago = PagoProveedorEntity.builder()
                .cuentaPorPagar(cuenta)
                .montoPagado(request.getMontoPagado())
                .fechaPago(request.getFechaPago() != null ? request.getFechaPago() : OffsetDateTime.now())
                .metodoPago(request.getMetodoPago() != null ? request.getMetodoPago() : "TRANSFERENCIA")
                .numeroComprobante(request.getNumeroComprobante())
                .soporteUrlArchivo(request.getSoporteUrlArchivo())
                .usuario(usuario)
                .observaciones(request.getObservaciones())
                .metadata(request.getMetadata() != null ? request.getMetadata() : "{}")
                .build();

        pagoProveedorRepository.save(pago);
        cuenta.getPagos().add(pago);
        CuentaPorPagarEntity guardada = cuentaPorPagarRepository.save(cuenta);

        // Si es periódica y quedó totalmente pagada, autogenerar la obligación del siguiente período
        if (cuenta.getEstado() == EstadoCuentaPagar.PAGADA && cuenta.getPeriodicidad() != PeriodicidadPago.PAGO_UNICO) {
            autogenerarSiguientePeriodo(cuenta);
        }

        return mapToResponse(guardada);
    }

    private void autogenerarSiguientePeriodo(CuentaPorPagarEntity cuentaPagada) {
        LocalDate proximoVencimiento = calcularProximaFecha(cuentaPagada.getFechaVencimiento(), cuentaPagada.getPeriodicidad());
        LocalDate proximaEmision = calcularProximaFecha(cuentaPagada.getFechaEmision(), cuentaPagada.getPeriodicidad());

        CuentaPorPagarEntity siguiente = CuentaPorPagarEntity.builder()
                .acreedor(cuentaPagada.getAcreedor())
                .numeroReferencia(cuentaPagada.getNumeroReferencia())
                .concepto(cuentaPagada.getConcepto())
                .montoTotal(cuentaPagada.getMontoTotal())
                .saldoPendiente(cuentaPagada.getMontoTotal())
                .tipoObligacion(cuentaPagada.getTipoObligacion())
                .periodicidad(cuentaPagada.getPeriodicidad())
                .fechaEmision(proximaEmision)
                .fechaVencimiento(proximoVencimiento)
                .diasAvisoAnticipado(cuentaPagada.getDiasAvisoAnticipado())
                .estado(EstadoCuentaPagar.PENDIENTE)
                .observaciones("Generado automáticamente por ciclo recurrente " + cuentaPagada.getPeriodicidad())
                .metadata(cuentaPagada.getMetadata())
                .pagos(new ArrayList<>())
                .build();

        cuentaPorPagarRepository.save(siguiente);
        log.info(">> [CUENTAS PAGAR] Próximo ciclo autogenerado para acreedor: {} con vencimiento: {}",
                cuentaPagada.getAcreedor().getRazonSocialONombre(), proximoVencimiento);
    }

    private LocalDate calcularProximaFecha(LocalDate fecha, PeriodicidadPago periodicidad) {
        if (fecha == null) fecha = LocalDate.now();
        return switch (periodicidad) {
            case MENSUAL -> fecha.plusMonths(1);
            case BIMESTRAL -> fecha.plusMonths(2);
            case TRIMESTRAL -> fecha.plusMonths(3);
            case SEMESTRAL -> fecha.plusMonths(6);
            case ANUAL -> fecha.plusYears(1);
            default -> fecha.plusMonths(1);
        };
    }

    public CuentaPorPagarResponseDto mapToResponse(CuentaPorPagarEntity entity) {
        LocalDate hoy = LocalDate.now();
        long dias = ChronoUnit.DAYS.between(hoy, entity.getFechaVencimiento());
        int aviso = entity.getDiasAvisoAnticipado() != null ? entity.getDiasAvisoAnticipado() : 5;

        String color;
        if (entity.getEstado() == EstadoCuentaPagar.PAGADA || entity.getEstado() == EstadoCuentaPagar.ANULADA) {
            color = "GRIS";
        } else if (dias <= 0) {
            color = "ROJO";
        } else if (dias <= aviso || dias <= 7) {
            color = "AMARILLO";
        } else {
            color = "VERDE";
        }

        List<PagoProveedorResponseDto> pagosDto = entity.getPagos() != null ?
                entity.getPagos().stream()
                        .map(p -> PagoProveedorResponseDto.builder()
                                .id(p.getId())
                                .cuentaPorPagarId(entity.getId())
                                .montoPagado(p.getMontoPagado())
                                .fechaPago(p.getFechaPago())
                                .metodoPago(p.getMetodoPago())
                                .numeroComprobante(p.getNumeroComprobante())
                                .soporteUrlArchivo(p.getSoporteUrlArchivo())
                                .usuarioId(p.getUsuario() != null ? p.getUsuario().getId() : null)
                                .usuarioNombre(p.getUsuario() != null ? p.getUsuario().getNombresApellidos() : null)
                                .observaciones(p.getObservaciones())
                                .metadata(p.getMetadata())
                                .createdAt(p.getCreatedAt())
                                .build())
                        .collect(Collectors.toList()) : new ArrayList<>();

        return CuentaPorPagarResponseDto.builder()
                .id(entity.getId())
                .acreedorTerceroId(entity.getAcreedor().getId())
                .acreedorDocumento(entity.getAcreedor().getNumeroDocumento())
                .acreedorNombre(entity.getAcreedor().getRazonSocialONombre())
                .acreedorCelular(entity.getAcreedor().getCelularPrincipal())
                .numeroReferencia(entity.getNumeroReferencia())
                .concepto(entity.getConcepto())
                .montoTotal(entity.getMontoTotal())
                .saldoPendiente(entity.getSaldoPendiente())
                .tipoObligacion(entity.getTipoObligacion())
                .periodicidad(entity.getPeriodicidad())
                .fechaEmision(entity.getFechaEmision())
                .fechaVencimiento(entity.getFechaVencimiento())
                .diasAvisoAnticipado(entity.getDiasAvisoAnticipado())
                .diasRestantes(dias)
                .colorSemaforo(color)
                .estado(entity.getEstado())
                .observaciones(entity.getObservaciones())
                .metadata(entity.getMetadata())
                .pagos(pagosDto)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
