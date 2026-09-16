package com.cdasanpedro.application.usecase.ingreso;

import com.cdasanpedro.application.dto.cliente.ClienteResponseDto;
import com.cdasanpedro.application.dto.ingreso.*;
import com.cdasanpedro.application.dto.vehiculo.VehiculoResponseDto;
import com.cdasanpedro.application.usecase.cliente.ClienteService;
import com.cdasanpedro.application.usecase.vehiculo.VehiculoService;
import com.cdasanpedro.core.exception.BusinessException;
import com.cdasanpedro.core.exception.ResourceNotFoundException;
import com.cdasanpedro.core.model.enums.EstadoOrden;
import com.cdasanpedro.core.model.enums.EstadoPrueba;
import com.cdasanpedro.core.model.enums.TipoPrueba;
import com.cdasanpedro.infrastructure.persistence.entity.*;
import com.cdasanpedro.infrastructure.persistence.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdenIngresoService {

    private final OrdenIngresoRepository ordenIngresoRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PruebaInspeccionRepository pruebaInspeccionRepository;
    private final FacturaRepository facturaRepository;
    private final ClienteService clienteService;
    private final VehiculoService vehiculoService;
    private final com.cdasanpedro.application.usecase.reinspeccion.ReinspeccionService reinspeccionService;

    @PersistenceContext
    private final EntityManager entityManager;

    @Transactional
    public OrdenIngresoResponseDto registrarIngreso(OrdenIngresoRequestDto request, UUID usuarioId) {
        // 1. Obtener el usuario recepcionista
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        // 2. Resolver o registrar el vehículo
        String placaLimpia = request.getPlaca().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        VehiculoEntity vehiculo = vehiculoRepository.findByPlaca(placaLimpia)
                .orElseGet(() -> {
                    if (request.getVehiculoData() != null) {
                        request.getVehiculoData().setPlaca(placaLimpia);
                        VehiculoResponseDto creado = vehiculoService.registrarOActualizar(request.getVehiculoData());
                        return vehiculoRepository.findById(creado.getId()).orElseThrow();
                    } else {
                        throw new BusinessException("El vehículo con placa " + placaLimpia + " no existe. Debe enviar los datos del vehículo.");
                    }
                });

        // 2.1 Actualizar o vincular propietario si viene en la solicitud
        if (request.getPropietarioId() != null) {
            ClienteEntity propietario = clienteRepository.findById(request.getPropietarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Propietario no encontrado con ID: " + request.getPropietarioId()));
            vehiculo.setPropietario(propietario);
            vehiculo = vehiculoRepository.save(vehiculo);
        } else if (request.getPropietarioData() != null && request.getPropietarioData().getNumeroDocumento() != null && !request.getPropietarioData().getNumeroDocumento().isBlank()) {
            ClienteResponseDto propDto = clienteService.registrarOActualizar(request.getPropietarioData());
            ClienteEntity propietario = clienteRepository.findById(propDto.getId()).orElseThrow();
            vehiculo.setPropietario(propietario);
            vehiculo = vehiculoRepository.save(vehiculo);
        }

        // 3. Resolver el conductor vs propietario
        ClienteEntity conductor = null;
        if (Boolean.TRUE.equals(request.getConductorEsPropietario())) {
            conductor = vehiculo.getPropietario();
            if (conductor == null && request.getConductorData() != null && request.getConductorData().getNumeroDocumento() != null) {
                // Registrar propietario y asignarlo
                ClienteResponseDto propDto = clienteService.registrarOActualizar(request.getConductorData());
                conductor = clienteRepository.findById(propDto.getId()).orElseThrow();
                vehiculo.setPropietario(conductor);
                vehiculo = vehiculoRepository.save(vehiculo);
            }
        } else {
            // El conductor es una persona diferente al propietario legal
            if (request.getConductorId() != null) {
                conductor = clienteRepository.findById(request.getConductorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + request.getConductorId()));
            } else if (request.getConductorData() != null && request.getConductorData().getNumeroDocumento() != null) {
                ClienteResponseDto conductorDto = clienteService.registrarOActualizar(request.getConductorData());
                conductor = clienteRepository.findById(conductorDto.getId()).orElseThrow();
            } else {
                throw new BusinessException("Debe especificar los datos o ID del conductor que ingresa el vehículo.");
            }
        }

        // 4. Crear la Orden de Ingreso
        OrdenIngresoEntity ordenPadre = null;
        if (request.getOrdenPadreId() != null) {
            ordenPadre = ordenIngresoRepository.findById(request.getOrdenPadreId()).orElse(null);
        }

        OrdenIngresoEntity orden = OrdenIngresoEntity.builder()
                .fechaIngreso(OffsetDateTime.now())
                .kilometraje(request.getKilometraje())
                .tipoServicio(request.getTipoServicio())
                .estado(EstadoOrden.INGRESADO)
                .conductorEsPropietario(request.getConductorEsPropietario())
                .conductor(conductor)
                .vehiculo(vehiculo)
                .usuario(usuario)
                .ordenPadre(ordenPadre)
                .esReinspeccion(Boolean.TRUE.equals(request.getEsReinspeccion()) || ordenPadre != null)
                .observaciones(request.getObservaciones())
                .build();

        OrdenIngresoEntity guardada = ordenIngresoRepository.saveAndFlush(orden);
        entityManager.refresh(guardada);

        if (guardada.getEsReinspeccion() && request.getOrdenPadreId() != null) {
            reinspeccionService.registrarReingreso(guardada.getId(), request.getOrdenPadreId());
        }

        // 5. Inicializar las 4 Pruebas Técnicas Reglamentarias
        List<PruebaInspeccionEntity> pruebasPadre = (ordenPadre != null)
                ? pruebaInspeccionRepository.findByOrdenIngresoIdOrderByCreatedAtAsc(ordenPadre.getId())
                : java.util.Collections.emptyList();

        for (TipoPrueba tipo : TipoPrueba.values()) {
            PruebaInspeccionEntity pruebaPadre = pruebasPadre.stream()
                    .filter(p -> p.getTipoPrueba() == tipo)
                    .findFirst()
                    .orElse(null);

            EstadoPrueba estadoInicial = EstadoPrueba.PENDIENTE;
            String obsInicial = null;

            if (pruebaPadre != null && pruebaPadre.getEstado() == EstadoPrueba.APROBADO) {
                estadoInicial = EstadoPrueba.APROBADO;
                obsInicial = "Aprobado en revisión inicial #" + ordenPadre.getConsecutivo();
            } else if (pruebaPadre != null && pruebaPadre.getEstado() == EstadoPrueba.RECHAZADO) {
                obsInicial = "Requiere reinspección obligatoria (Reprobado en orden #" + ordenPadre.getConsecutivo() + ")";
            }

            PruebaInspeccionEntity prueba = PruebaInspeccionEntity.builder()
                    .ordenIngreso(guardada)
                    .tipoPrueba(tipo)
                    .estado(estadoInicial)
                    .observaciones(obsInicial)
                    .usuarioResponsable(estadoInicial == EstadoPrueba.APROBADO ? usuario : null)
                    .fechaEjecucion(estadoInicial == EstadoPrueba.APROBADO ? OffsetDateTime.now() : null)
                    .build();
            pruebaInspeccionRepository.save(prueba);
        }

        return toDto(guardada);
    }

    @Transactional
    public PruebaInspeccionResponseDto registrarResultadoPrueba(UUID ordenId, PruebaInspeccionRequestDto request, UUID usuarioId) {
        OrdenIngresoEntity orden = ordenIngresoRepository.findById(ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de ingreso no encontrada con ID: " + ordenId));

        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        PruebaInspeccionEntity prueba = pruebaInspeccionRepository.findByOrdenIngresoIdAndTipoPrueba(ordenId, request.getTipoPrueba())
                .orElseGet(() -> PruebaInspeccionEntity.builder()
                        .ordenIngreso(orden)
                        .tipoPrueba(request.getTipoPrueba())
                        .build());

        prueba.setEstado(request.getEstado());
        prueba.setObservaciones(request.getObservaciones());
        prueba.setUsuarioResponsable(usuario);
        prueba.setFechaEjecucion(OffsetDateTime.now());

        PruebaInspeccionEntity guardada = pruebaInspeccionRepository.save(prueba);

        // Transición de estado en la orden
        if (orden.getEstado() == EstadoOrden.INGRESADO) {
            orden.setEstado(EstadoOrden.EN_INSPECCION);
            ordenIngresoRepository.save(orden);
        }

        // Evaluar progreso general de las 4 pruebas
        List<PruebaInspeccionEntity> todas = pruebaInspeccionRepository.findByOrdenIngresoIdOrderByCreatedAtAsc(ordenId);
        boolean hayRechazo = todas.stream().anyMatch(p -> p.getEstado() == EstadoPrueba.RECHAZADO);
        boolean todasAprobadas = todas.size() == 4 && todas.stream().allMatch(p -> p.getEstado() == EstadoPrueba.APROBADO);

        if (hayRechazo) {
            orden.setEstado(EstadoOrden.RECHAZADO);
            ordenIngresoRepository.save(orden);
            reinspeccionService.registrarRechazo(orden.getId(), "Rechazado en inspección de pista");
        } else if (todasAprobadas) {
            orden.setEstado(EstadoOrden.APROBADO);
            ordenIngresoRepository.save(orden);
        }

        return toPruebaDto(guardada);
    }

    @Transactional(readOnly = true)
    public List<PruebaInspeccionResponseDto> listarPruebas(UUID ordenId) {
        return pruebaInspeccionRepository.findByOrdenIngresoIdOrderByCreatedAtAsc(ordenId)
                .stream()
                .map(this::toPruebaDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrdenIngresoResponseDto> listarListosParaFacturar() {
        return ordenIngresoRepository.findAll()
                .stream()
                .filter(o -> o.getEstado() != EstadoOrden.CANCELADO && o.getEstado() != EstadoOrden.FACTURADO)
                .filter(o -> facturaRepository.findByOrdenIngresoId(o.getId()).isEmpty())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private static final java.time.ZoneId ZONE_COLOMBIA = java.time.ZoneId.of("America/Bogota");

    @Transactional(readOnly = true)
    public List<OrdenIngresoResponseDto> listarIngresosHoy() {
        LocalDate hoy = LocalDate.now(ZONE_COLOMBIA);
        OffsetDateTime start = hoy.atStartOfDay(ZONE_COLOMBIA).toOffsetDateTime();
        OffsetDateTime end = hoy.plusDays(1).atStartOfDay(ZONE_COLOMBIA).toOffsetDateTime();

        return ordenIngresoRepository.findByFechaIngresoBetween(start, end)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrdenIngresoResponseDto> listarTodos() {
        return ordenIngresoRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrdenIngresoResponseDto obtenerPorId(UUID id) {
        OrdenIngresoEntity entity = ordenIngresoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de ingreso no encontrada con ID: " + id));
        return toDto(entity);
    }

    @Transactional
    public OrdenIngresoResponseDto cambiarEstado(UUID id, EstadoOrden nuevoEstado) {
        return cambiarEstado(id, nuevoEstado, null);
    }

    @Transactional
    public OrdenIngresoResponseDto cambiarEstado(UUID id, EstadoOrden nuevoEstado, String observaciones) {
        return cambiarEstado(id, nuevoEstado, observaciones, null);
    }

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Transactional
    public OrdenIngresoResponseDto rechazarOrden(UUID id, RechazoOrdenRequestDto request, UUID usuarioId) {
        OrdenIngresoEntity entity = ordenIngresoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de ingreso no encontrada con ID: " + id));

        UsuarioEntity usuario = usuarioId != null 
                ? usuarioRepository.findById(usuarioId).orElse(entity.getUsuario())
                : entity.getUsuario();

        entity.setEstado(EstadoOrden.RECHAZADO);
        entity.setObservaciones(request.getMotivo() != null ? request.getMotivo().trim() : "Rechazado en inspección");

        // Construir metadata con motivo, evidencia y pruebas reprobadas
        List<String> nombresPruebas = request.getPruebasRechazadas() != null
                ? request.getPruebasRechazadas().stream().map(Enum::name).collect(Collectors.toList())
                : java.util.Collections.emptyList();

        java.util.Map<String, Object> metaMap = new java.util.HashMap<>();
        metaMap.put("motivoRechazo", request.getMotivo());
        metaMap.put("evidenciaRechazo", request.getEvidencia());
        metaMap.put("pruebasRechazadas", nombresPruebas);
        metaMap.put("fechaRechazo", OffsetDateTime.now().toString());
        metaMap.put("usuarioRechazo", usuario != null ? usuario.getNombresApellidos() : "Sistema");

        String jsonMeta = "{}";
        try {
            jsonMeta = objectMapper.writeValueAsString(metaMap);
            entity.setMetadata(jsonMeta);
        } catch (Exception ignored) {
        }

        OrdenIngresoEntity guardada = ordenIngresoRepository.save(entity);

        // Actualizar estados individuales de pruebas
        List<PruebaInspeccionEntity> pruebas = pruebaInspeccionRepository.findByOrdenIngresoIdOrderByCreatedAtAsc(id);
        for (PruebaInspeccionEntity p : pruebas) {
            if (request.getPruebasRechazadas() != null && request.getPruebasRechazadas().contains(p.getTipoPrueba())) {
                p.setEstado(EstadoPrueba.RECHAZADO);
                p.setObservaciones(request.getMotivo());
            } else if (p.getEstado() == EstadoPrueba.PENDIENTE) {
                p.setEstado(EstadoPrueba.APROBADO);
            }
            p.setUsuarioResponsable(usuario);
            p.setFechaEjecucion(OffsetDateTime.now());
            pruebaInspeccionRepository.save(p);
        }

        reinspeccionService.registrarRechazo(guardada.getId(), jsonMeta);

        return toDto(guardada);
    }

    @Transactional
    public OrdenIngresoResponseDto cambiarEstado(UUID id, EstadoOrden nuevoEstado, String observaciones, UUID usuarioId) {
        OrdenIngresoEntity entity = ordenIngresoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de ingreso no encontrada con ID: " + id));

        UsuarioEntity usuario = usuarioId != null ? usuarioRepository.findById(usuarioId).orElse(null) : null;

        entity.setEstado(nuevoEstado);
        if (observaciones != null && !observaciones.isBlank()) {
            String obsActual = entity.getObservaciones() != null && !entity.getObservaciones().isBlank() 
                    ? entity.getObservaciones() + " | " 
                    : "";
            entity.setObservaciones(obsActual + observaciones.trim());
        }

        // Si se aprueba, por defecto se aprueban todas las pruebas pendientes automáticamente
        if (nuevoEstado == EstadoOrden.APROBADO) {
            List<PruebaInspeccionEntity> pruebas = pruebaInspeccionRepository.findByOrdenIngresoIdOrderByCreatedAtAsc(id);
            for (PruebaInspeccionEntity p : pruebas) {
                if (p.getEstado() == EstadoPrueba.PENDIENTE) {
                    p.setEstado(EstadoPrueba.APROBADO);
                    p.setUsuarioResponsable(usuario != null ? usuario : entity.getUsuario());
                    p.setFechaEjecucion(OffsetDateTime.now());
                    pruebaInspeccionRepository.save(p);
                }
            }
        }

        OrdenIngresoEntity actualizada = ordenIngresoRepository.save(entity);

        if (nuevoEstado == EstadoOrden.RECHAZADO) {
            reinspeccionService.registrarRechazo(actualizada.getId(), observaciones != null ? observaciones : "Rechazado");
        }

        return toDto(actualizada);
    }

    public OrdenIngresoResponseDto toDto(OrdenIngresoEntity entity) {
        VehiculoResponseDto vehDto = vehiculoService.toDto(entity.getVehiculo());
        ClienteResponseDto condDto = entity.getConductor() != null 
                ? clienteService.toDto(entity.getConductor()) 
                : null;

        Long diasRestantes = null;
        Boolean esVigente = null;
        OffsetDateTime fechaLimite = null;
        Long consecutivoPadre = entity.getOrdenPadre() != null ? entity.getOrdenPadre().getConsecutivo() : null;
        List<String> pruebasFallidasPadre = null;
        String motivoRechazo = null;
        String evidenciaRechazo = null;
        List<String> pruebasRechazadas = new java.util.ArrayList<>();

        // Extraer motivo y evidencia de rechazo si aplica
        if (entity.getEstado() == EstadoOrden.RECHAZADO) {
            OffsetDateTime fechaBase = entity.getFechaIngreso() != null ? entity.getFechaIngreso() : entity.getCreatedAt();
            fechaLimite = fechaBase.plusDays(15);
            long transcurridos = java.time.temporal.ChronoUnit.DAYS.between(fechaBase, OffsetDateTime.now());
            long restantes = java.time.temporal.ChronoUnit.DAYS.between(OffsetDateTime.now(), fechaLimite);
            diasRestantes = Math.max(0, restantes);
            esVigente = restantes >= 0 && transcurridos <= 15;

            // Pruebas fallidas de esta misma orden
            pruebasRechazadas = pruebaInspeccionRepository.findByOrdenIngresoIdOrderByCreatedAtAsc(entity.getId())
                    .stream()
                    .filter(p -> p.getEstado() == EstadoPrueba.RECHAZADO)
                    .map(p -> p.getTipoPrueba().name())
                    .collect(Collectors.toList());

            if (entity.getMetadata() != null && !entity.getMetadata().isBlank()) {
                try {
                    com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(entity.getMetadata());
                    if (node.hasNonNull("motivoRechazo")) {
                        motivoRechazo = node.get("motivoRechazo").asText();
                    } else if (node.hasNonNull("motivo")) {
                        motivoRechazo = node.get("motivo").asText();
                    }
                    if (node.hasNonNull("evidenciaRechazo")) {
                        evidenciaRechazo = node.get("evidenciaRechazo").asText();
                    } else if (node.hasNonNull("evidencia")) {
                        evidenciaRechazo = node.get("evidencia").asText();
                    }
                } catch (Exception ignored) {
                }
            }
            if (motivoRechazo == null) {
                motivoRechazo = entity.getObservaciones();
            }
        }

        if (entity.getOrdenPadre() != null) {
            pruebasFallidasPadre = pruebaInspeccionRepository.findByOrdenIngresoIdOrderByCreatedAtAsc(entity.getOrdenPadre().getId())
                    .stream()
                    .filter(p -> p.getEstado() == EstadoPrueba.RECHAZADO)
                    .map(p -> p.getTipoPrueba().name())
                    .collect(Collectors.toList());
        }

        // Factura asociada
        Optional<FacturaEntity> optFactura = facturaRepository.findByOrdenIngresoId(entity.getId());
        boolean facturado = optFactura.isPresent() || entity.getEstado() == EstadoOrden.FACTURADO;
        UUID facturaId = optFactura.map(FacturaEntity::getId).orElse(null);
        String numeroFactura = optFactura.map(FacturaEntity::getNumeroFactura).orElse(null);

        return OrdenIngresoResponseDto.builder()
                .id(entity.getId())
                .consecutivo(entity.getConsecutivo())
                .fechaIngreso(entity.getFechaIngreso())
                .kilometraje(entity.getKilometraje())
                .tipoServicio(entity.getTipoServicio())
                .estado(entity.getEstado())
                .conductorEsPropietario(entity.getConductorEsPropietario())
                .ordenPadreId(entity.getOrdenPadre() != null ? entity.getOrdenPadre().getId() : null)
                .consecutivoOrdenPadre(consecutivoPadre)
                .esReinspeccion(entity.getEsReinspeccion())
                .diasTranscurridosRechazo(entity.getDiasTranscurridosRechazo())
                .diasRestantesReinspeccion(diasRestantes)
                .esReinspeccionVigente(esVigente)
                .fechaLimiteReinspeccion(fechaLimite)
                .pruebasRechazadasPrevias(pruebasFallidasPadre)
                .motivoRechazo(motivoRechazo)
                .evidenciaRechazo(evidenciaRechazo)
                .pruebasRechazadas(pruebasRechazadas)
                .vehiculo(vehDto)
                .conductor(condDto)
                .usuarioNombre(entity.getUsuario() != null ? entity.getUsuario().getNombresApellidos() : "Sistema")
                .observaciones(entity.getObservaciones())
                .facturado(facturado)
                .facturaId(facturaId)
                .numeroFactura(numeroFactura)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public PruebaInspeccionResponseDto toPruebaDto(PruebaInspeccionEntity entity) {
        return PruebaInspeccionResponseDto.builder()
                .id(entity.getId())
                .ordenIngresoId(entity.getOrdenIngreso() != null ? entity.getOrdenIngreso().getId() : null)
                .tipoPrueba(entity.getTipoPrueba())
                .estado(entity.getEstado())
                .observaciones(entity.getObservaciones())
                .usuarioResponsableId(entity.getUsuarioResponsable() != null ? entity.getUsuarioResponsable().getId() : null)
                .usuarioResponsableNombre(entity.getUsuarioResponsable() != null ? entity.getUsuarioResponsable().getNombresApellidos() : null)
                .usuarioResponsableRol(entity.getUsuarioResponsable() != null && entity.getUsuarioResponsable().getRol() != null ? entity.getUsuarioResponsable().getRol().name() : null)
                .fechaEjecucion(entity.getFechaEjecucion())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
