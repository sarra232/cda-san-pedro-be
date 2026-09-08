package com.cdasanpedro.application.usecase.reinspeccion;

import com.cdasanpedro.application.dto.reinspeccion.ReinspeccionSeguimientoResponseDto;
import com.cdasanpedro.application.dto.reinspeccion.ReinspeccionVerificacionDto;
import com.cdasanpedro.core.model.enums.EstadoReinspeccion;
import com.cdasanpedro.infrastructure.persistence.entity.ClienteEntity;
import com.cdasanpedro.infrastructure.persistence.entity.NotificacionEntity;
import com.cdasanpedro.infrastructure.persistence.entity.OrdenIngresoEntity;
import com.cdasanpedro.infrastructure.persistence.entity.ReinspeccionSeguimientoEntity;
import com.cdasanpedro.infrastructure.persistence.repository.NotificacionRepository;
import com.cdasanpedro.infrastructure.persistence.repository.OrdenIngresoRepository;
import com.cdasanpedro.infrastructure.persistence.repository.ReinspeccionSeguimientoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cdasanpedro.infrastructure.persistence.repository.PruebaInspeccionRepository;
import com.cdasanpedro.core.model.enums.EstadoPrueba;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReinspeccionService {

    private final ReinspeccionSeguimientoRepository reinspeccionRepository;
    private final OrdenIngresoRepository ordenIngresoRepository;
    private final NotificacionRepository notificacionRepository;
    private final PruebaInspeccionRepository pruebaInspeccionRepository;

    @Transactional(readOnly = true)
    public List<ReinspeccionSeguimientoResponseDto> listarSeguimientos() {
        return reinspeccionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReinspeccionVerificacionDto verificarElegibilidad(String placa) {
        if (placa == null || placa.isBlank()) {
            return ReinspeccionVerificacionDto.builder().tieneReinspeccionGratuita(false).build();
        }

        String placaNormalizada = placa.trim().toUpperCase();
        List<ReinspeccionSeguimientoEntity> pendientes = reinspeccionRepository.findPendientesPorPlaca(placaNormalizada);

        if (pendientes.isEmpty()) {
            return ReinspeccionVerificacionDto.builder()
                    .tieneReinspeccionGratuita(false)
                    .placa(placaNormalizada)
                    .mensaje("No registra inspecciones previas pendientes de reinspección.")
                    .build();
        }

        ReinspeccionSeguimientoEntity seguimiento = pendientes.get(0);
        OffsetDateTime ahora = OffsetDateTime.now();
        long diasTranscurridos = ChronoUnit.DAYS.between(seguimiento.getFechaRechazo(), ahora);
        long diasRestantes = ChronoUnit.DAYS.between(ahora, seguimiento.getFechaLimite15Dias());

        boolean vigente = diasRestantes >= 0 && diasTranscurridos <= 15;

        List<String> pruebasFallidas = java.util.Collections.emptyList();
        if (seguimiento.getOrdenRechazada() != null) {
            pruebasFallidas = pruebaInspeccionRepository.findByOrdenIngresoIdOrderByCreatedAtAsc(seguimiento.getOrdenRechazada().getId())
                    .stream()
                    .filter(p -> p.getEstado() == EstadoPrueba.RECHAZADO)
                    .map(p -> p.getTipoPrueba().name())
                    .collect(Collectors.toList());
        }

        return ReinspeccionVerificacionDto.builder()
                .tieneReinspeccionGratuita(vigente)
                .ordenRechazadaId(seguimiento.getOrdenRechazada().getId())
                .consecutivoOrdenRechazada(seguimiento.getOrdenRechazada().getConsecutivo())
                .placa(placaNormalizada)
                .fechaRechazo(seguimiento.getFechaRechazo())
                .fechaLimite15Dias(seguimiento.getFechaLimite15Dias())
                .diasTranscurridos(diasTranscurridos)
                .diasRestantes(Math.max(0, diasRestantes))
                .pruebasRechazadas(pruebasFallidas)
                .mensaje(vigente ?
                        "Reinspección gratuita vigente (Día " + (diasTranscurridos + 1) + " de 15. Quedan " + diasRestantes + " días)." :
                        "Plazo legal de 15 días calendario vencido hace " + Math.abs(diasRestantes) + " días.")
                .build();
    }

    @Transactional
    public ReinspeccionSeguimientoResponseDto registrarRechazo(UUID ordenId, String defectosJson) {
        OrdenIngresoEntity orden = ordenIngresoRepository.findById(ordenId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada con ID: " + ordenId));

        Optional<ReinspeccionSeguimientoEntity> existente = reinspeccionRepository.findByOrdenRechazadaId(ordenId);
        if (existente.isPresent()) {
            return mapToResponse(existente.get());
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        OffsetDateTime fechaLimite = ahora.plusDays(15);

        String jsonMetadata = "{}";
        if (defectosJson != null && !defectosJson.isBlank()) {
            String trimmed = defectosJson.trim();
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                jsonMetadata = trimmed;
            } else {
                String safeObs = trimmed.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
                jsonMetadata = String.format("{\"motivo\":\"%s\"}", safeObs);
            }
        }

        ClienteEntity cliente = orden.getConductor() != null 
                ? orden.getConductor() 
                : (orden.getVehiculo() != null ? orden.getVehiculo().getPropietario() : null);

        ReinspeccionSeguimientoEntity seguimiento = ReinspeccionSeguimientoEntity.builder()
                .ordenRechazada(orden)
                .vehiculo(orden.getVehiculo())
                .cliente(cliente)
                .fechaRechazo(ahora)
                .fechaLimite15Dias(fechaLimite)
                .estadoSeguimiento(EstadoReinspeccion.EN_PLAZO)
                .reinspeccionCompletada(false)
                .metadata(jsonMetadata)
                .build();

        ReinspeccionSeguimientoEntity guardado = reinspeccionRepository.save(seguimiento);
        log.info(">> [REINSPECCION] Seguimiento de 15 días iniciado para placa: {} hasta: {}",
                orden.getVehiculo().getPlaca(), fechaLimite);

        // Programar las 5 alertas escalonadas: Faltando 10, 5, 3, 2 y 0 días
        programarAlertasEscalonadas(guardado);

        return mapToResponse(guardado);
    }

    private void programarAlertasEscalonadas(ReinspeccionSeguimientoEntity seguimiento) {
        if (seguimiento.getCliente() == null) return;

        String celular = seguimiento.getCliente().getCelular();
        String placa = seguimiento.getVehiculo().getPlaca();
        OffsetDateTime rechazo = seguimiento.getFechaRechazo();

        // 1. Faltan 10 días (Día 5)
        crearAlerta(seguimiento.getCliente(), "WHATSAPP", celular,
                "CDA San Pedro: Recordatorio 2da Revisión Gratuita",
                String.format("{\"placa\":\"%s\",\"dias_restantes\":10,\"tipo\":\"REINSPECCION_10_DIAS\"}", placa),
                rechazo.plusDays(5));

        // 2. Faltan 5 días (Día 10)
        crearAlerta(seguimiento.getCliente(), "WHATSAPP", celular,
                "CDA San Pedro: Faltan 5 días para su Re-inspección sin costo",
                String.format("{\"placa\":\"%s\",\"dias_restantes\":5,\"tipo\":\"REINSPECCION_5_DIAS\"}", placa),
                rechazo.plusDays(10));

        // 3. Faltan 3 días (Día 12)
        crearAlerta(seguimiento.getCliente(), "WHATSAPP", celular,
                "CDA San Pedro: ¡Atención! Solo quedan 3 días para su revisión gratuita",
                String.format("{\"placa\":\"%s\",\"dias_restantes\":3,\"tipo\":\"REINSPECCION_3_DIAS\"}", placa),
                rechazo.plusDays(12));

        // 4. Faltan 2 días (Día 13)
        crearAlerta(seguimiento.getCliente(), "WHATSAPP", celular,
                "CDA San Pedro: ¡Urgente! Faltan 2 días para vencer el plazo de reinspección",
                String.format("{\"placa\":\"%s\",\"dias_restantes\":2,\"tipo\":\"REINSPECCION_2_DIAS\"}", placa),
                rechazo.plusDays(13));

        // 5. Mismo día de vencimiento (Día 15)
        crearAlerta(seguimiento.getCliente(), "WHATSAPP", celular,
                "CDA San Pedro: ¡ÚLTIMO DÍA! Hoy vence su plazo de 2da revisión sin costo",
                String.format("{\"placa\":\"%s\",\"dias_restantes\":0,\"tipo\":\"REINSPECCION_HOY\"}", placa),
                rechazo.plusDays(15));
    }

    private void crearAlerta(ClienteEntity cliente, String canal, String destinatario, String asunto, String payload, OffsetDateTime fechaProgramada) {
        if (destinatario == null || destinatario.isBlank()) return;

        NotificacionEntity notif = NotificacionEntity.builder()
                .cliente(cliente)
                .tipo("RECORDATORIO_REINSPECCION")
                .canal(canal)
                .destinatario(destinatario)
                .asunto(asunto)
                .cuerpoPayload(payload)
                .estado("PENDIENTE")
                .intentos(0)
                .fechaProgramada(fechaProgramada)
                .build();

        notificacionRepository.save(notif);
    }

    @Transactional
    public void registrarReingreso(UUID ordenReinspeccionId, UUID ordenRechazadaId) {
        Optional<ReinspeccionSeguimientoEntity> seguimientoOpt = reinspeccionRepository.findByOrdenRechazadaId(ordenRechazadaId);
        if (seguimientoOpt.isPresent()) {
            ReinspeccionSeguimientoEntity seguimiento = seguimientoOpt.get();
            OrdenIngresoEntity ordenReinspeccion = ordenIngresoRepository.findById(ordenReinspeccionId).orElse(null);

            seguimiento.setReinspeccionCompletada(true);
            seguimiento.setEstadoSeguimiento(EstadoReinspeccion.REINSPECCIONADO);
            seguimiento.setOrdenReinspeccion(ordenReinspeccion);
            reinspeccionRepository.save(seguimiento);

            if (seguimiento.getCliente() != null && seguimiento.getCliente().getId() != null) {
                int canceladas = notificacionRepository.cancelarPendientesPorClienteYTipo(
                        seguimiento.getCliente().getId(),
                        "RECORDATORIO_REINSPECCION"
                );
                log.info(">> [REINSPECCION] Reingreso completado para placa: {}. {} alertas de reinspección canceladas.",
                        seguimiento.getVehiculo().getPlaca(), canceladas);
            }
        }
    }

    public ReinspeccionSeguimientoResponseDto mapToResponse(ReinspeccionSeguimientoEntity entity) {
        OffsetDateTime ahora = OffsetDateTime.now();
        long diasRestantes = ChronoUnit.DAYS.between(ahora, entity.getFechaLimite15Dias());

        return ReinspeccionSeguimientoResponseDto.builder()
                .id(entity.getId())
                .ordenRechazadaId(entity.getOrdenRechazada().getId())
                .consecutivoOrdenRechazada(entity.getOrdenRechazada().getConsecutivo())
                .vehiculoId(entity.getVehiculo().getId())
                .vehiculoPlaca(entity.getVehiculo().getPlaca())
                .vehiculoMarca(entity.getVehiculo().getMarca())
                .vehiculoLinea(entity.getVehiculo().getLinea())
                .clienteNombre(entity.getCliente() != null ? entity.getCliente().getNombresRazonSocial() : null)
                .clienteCelular(entity.getCliente() != null ? entity.getCliente().getCelular() : null)
                .clienteEmail(entity.getCliente() != null ? entity.getCliente().getEmail() : null)
                .fechaRechazo(entity.getFechaRechazo())
                .fechaLimite15Dias(entity.getFechaLimite15Dias())
                .diasRestantes(Math.max(0, diasRestantes))
                .estadoSeguimiento(entity.getEstadoSeguimiento())
                .reinspeccionCompletada(entity.getReinspeccionCompletada())
                .ordenReinspeccionId(entity.getOrdenReinspeccion() != null ? entity.getOrdenReinspeccion().getId() : null)
                .consecutivoOrdenReinspeccion(entity.getOrdenReinspeccion() != null ? entity.getOrdenReinspeccion().getConsecutivo() : null)
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
