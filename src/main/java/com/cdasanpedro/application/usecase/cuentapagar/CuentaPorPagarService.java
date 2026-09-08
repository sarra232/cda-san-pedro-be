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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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
