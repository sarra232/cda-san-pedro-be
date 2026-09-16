package com.cdasanpedro.application.usecase.hr;

import com.cdasanpedro.application.dto.hr.*;
import com.cdasanpedro.core.exception.BusinessException;
import com.cdasanpedro.core.exception.ResourceNotFoundException;
import com.cdasanpedro.core.gateway.NotificationGateway;
import com.cdasanpedro.core.model.enums.*;
import com.cdasanpedro.infrastructure.notification.EmailTemplateBuilder;
import com.cdasanpedro.infrastructure.persistence.entity.*;
import com.cdasanpedro.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final EmpleadoCertificacionRepository certificacionRepository;
    private final TerceroRepository terceroRepository;
    private final TerceroRolRepository terceroRolRepository;
    private final UsuarioRepository usuarioRepository;
    private final TokenAutenticacionRepository tokenRepository;
    private final NotificationGateway notificationGateway;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public List<EmpleadoResponseDto> listarTodos(EstadoEmpleado estado) {
        List<EmpleadoEntity> lista = (estado != null)
                ? empleadoRepository.findByEstado(estado)
                : empleadoRepository.findAllWithTercero();

        return lista.stream().map(this::mapToEmpleadoDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmpleadoResponseDto obtenerPorId(UUID id) {
        EmpleadoEntity entity = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con ID: " + id));
        return mapToEmpleadoDto(entity);
    }

    @Transactional
    public EmpleadoResponseDto crear(EmpleadoRequestDto request) {
        TerceroEntity tercero;
        if (request.getTerceroId() != null) {
            tercero = terceroRepository.findById(request.getTerceroId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tercero no encontrado con ID: " + request.getTerceroId()));
        } else {
            String doc = request.getNumeroDocumento() != null ? request.getNumeroDocumento().trim() : "00000000";
            TipoDocumento tipoDoc = TipoDocumento.CC;
            if (request.getTipoDocumento() != null) {
                try {
                    tipoDoc = TipoDocumento.valueOf(request.getTipoDocumento().trim().toUpperCase());
                } catch (Exception ignored) {}
            }

            final TipoDocumento finalTipoDoc = tipoDoc;
            tercero = terceroRepository.findByNumeroDocumento(doc)
                    .orElseGet(() -> {
                        TerceroEntity t = TerceroEntity.builder()
                                .tipoDocumento(finalTipoDoc)
                                .numeroDocumento(doc)
                                .razonSocialONombre(request.getNombresApellidos() != null ? request.getNombresApellidos().trim() : "Empleado CDA")
                                .celularPrincipal(request.getCelular() != null ? request.getCelular().trim() : "3000000000")
                                .emailPrincipal(request.getEmail() != null ? request.getEmail().trim() : null)
                                .direccion(request.getDireccion() != null ? request.getDireccion().trim() : null)
                                .build();
                        return terceroRepository.save(t);
                    });
        }

        // Asignar rol polimórfico EMPLEADO
        if (terceroRolRepository.findByTerceroIdAndTipoRol(tercero.getId(), TipoRolTercero.EMPLEADO).isEmpty()) {
            terceroRolRepository.save(TerceroRolEntity.builder()
                    .tercero(tercero)
                    .tipoRol(TipoRolTercero.EMPLEADO)
                    .activo(true)
                    .build());
        }

        EmpleadoEntity entity = EmpleadoEntity.builder()
                .tercero(tercero)
                .cargo(request.getCargo().trim().toUpperCase())
                .departamento(request.getDepartamento() != null ? request.getDepartamento().trim() : "OPERACIONES_PISTA")
                .tipoContrato(request.getTipoContrato() != null ? request.getTipoContrato() : TipoContrato.TERMINO_INDEFINIDO)
                .salarioBase(request.getSalarioBase() != null ? request.getSalarioBase() : new BigDecimal("1600000.00"))
                .auxilioTransporteAplica(request.getAuxilioTransporteAplica() != null ? request.getAuxilioTransporteAplica() : true)
                .banco(request.getBanco())
                .tipoCuenta(request.getTipoCuenta() != null ? request.getTipoCuenta() : "AHORROS")
                .numeroCuenta(request.getNumeroCuenta())
                .fechaIngreso(request.getFechaIngreso() != null ? request.getFechaIngreso() : LocalDate.now())
                .estado(request.getEstado() != null ? request.getEstado() : EstadoEmpleado.ACTIVO)
                .metadata(request.getMetadata() != null ? request.getMetadata() : "{}")
                .certificaciones(new ArrayList<>())
                .build();

        EmpleadoEntity savedEmpleado = empleadoRepository.save(entity);

        // ONBOARDING DE SEGURIDAD: Crear o vincular cuenta de usuario y enviar invitación por correo
        RolUsuario rol = (request.getRolApp() != null) ? request.getRolApp() : mapearCargoARol(savedEmpleado.getCargo());
        if (savedEmpleado.getEstado() == EstadoEmpleado.ACTIVO && tercero.getEmailPrincipal() != null && !tercero.getEmailPrincipal().isBlank()) {
            enviarInvitacionEmpleado(savedEmpleado, tercero, rol);
        } else if (tercero.getNumeroDocumento() != null) {
            // Asegurar creación o actualización de usuario aunque no tenga correo inmediato
            final RolUsuario finalRol = rol;
            usuarioRepository.findByNumeroDocumento(tercero.getNumeroDocumento())
                    .ifPresentOrElse(
                            u -> {
                                if (request.getRolApp() != null) u.setRol(finalRol);
                                u.setActivo(savedEmpleado.getEstado() == EstadoEmpleado.ACTIVO);
                                usuarioRepository.save(u);
                            },
                            () -> {
                                UsuarioEntity nuevo = UsuarioEntity.builder()
                                        .tipoDocumento(tercero.getTipoDocumento() != null ? tercero.getTipoDocumento() : TipoDocumento.CC)
                                        .numeroDocumento(tercero.getNumeroDocumento())
                                        .nombresApellidos(tercero.getRazonSocialONombre())
                                        .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                                        .rol(finalRol)
                                        .activo(savedEmpleado.getEstado() == EstadoEmpleado.ACTIVO)
                                        .build();
                                usuarioRepository.save(nuevo);
                            }
                    );
        }

        return mapToEmpleadoDto(savedEmpleado);
    }

    @Transactional
    public EmpleadoResponseDto actualizar(UUID id, EmpleadoRequestDto request) {
        EmpleadoEntity entity = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con ID: " + id));

        if (request.getCargo() != null) entity.setCargo(request.getCargo().trim().toUpperCase());
        if (request.getDepartamento() != null) entity.setDepartamento(request.getDepartamento().trim());
        if (request.getTipoContrato() != null) entity.setTipoContrato(request.getTipoContrato());
        if (request.getSalarioBase() != null) entity.setSalarioBase(request.getSalarioBase());
        if (request.getAuxilioTransporteAplica() != null) entity.setAuxilioTransporteAplica(request.getAuxilioTransporteAplica());
        if (request.getBanco() != null) entity.setBanco(request.getBanco());
        if (request.getTipoCuenta() != null) entity.setTipoCuenta(request.getTipoCuenta());
        if (request.getNumeroCuenta() != null) entity.setNumeroCuenta(request.getNumeroCuenta());
        if (request.getFechaRetiro() != null) entity.setFechaRetiro(request.getFechaRetiro());
        if (request.getEstado() != null) {
            entity.setEstado(request.getEstado());
        }

        // Sincronizar estado y rol con UsuarioEntity si existe
        if (entity.getTercero() != null) {
            usuarioRepository.findByNumeroDocumento(entity.getTercero().getNumeroDocumento())
                    .ifPresent(u -> {
                        if (request.getRolApp() != null) {
                            u.setRol(request.getRolApp());
                        } else if (request.getCargo() != null) {
                            u.setRol(mapearCargoARol(entity.getCargo()));
                        }
                        if (request.getEstado() != null) {
                            boolean isActivo = (entity.getEstado() == EstadoEmpleado.ACTIVO);
                            u.setActivo(isActivo);
                        }
                        if (request.getNombresApellidos() != null) {
                            u.setNombresApellidos(request.getNombresApellidos().trim());
                        }
                        usuarioRepository.save(u);
                    });
        }

        // Actualizar datos del tercero si vinieron
        if (entity.getTercero() != null) {
            if (request.getNombresApellidos() != null) entity.getTercero().setRazonSocialONombre(request.getNombresApellidos().trim());
            if (request.getCelular() != null) entity.getTercero().setCelularPrincipal(request.getCelular().trim());
            if (request.getEmail() != null) entity.getTercero().setEmailPrincipal(request.getEmail().trim());
            if (request.getDireccion() != null) entity.getTercero().setDireccion(request.getDireccion().trim());
            terceroRepository.save(entity.getTercero());
        }

        return mapToEmpleadoDto(empleadoRepository.save(entity));
    }

    @Transactional
    public void eliminar(UUID id) {
        EmpleadoEntity entity = empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con ID: " + id));
        entity.setEstado(EstadoEmpleado.RETIRADO);
        entity.setFechaRetiro(LocalDate.now());
        empleadoRepository.save(entity);

        // Desactivar usuario del sistema correspondiente en cascada
        if (entity.getTercero() != null) {
            usuarioRepository.findByNumeroDocumento(entity.getTercero().getNumeroDocumento())
                    .ifPresent(u -> {
                        u.setActivo(false);
                        usuarioRepository.save(u);
                    });
        }
    }

    /**
     * Permite a un Administrador reenviar la invitación de activación a un colaborador (o reactivar a un colaborador inactivo y enviarle enlace para crear clave).
     */
    @Transactional
    public String reenviarInvitacion(UUID empleadoId) {
        EmpleadoEntity empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con ID: " + empleadoId));

        TerceroEntity tercero = empleado.getTercero();
        if (tercero == null || tercero.getEmailPrincipal() == null || tercero.getEmailPrincipal().isBlank()) {
            throw new BusinessException("El colaborador no tiene un correo electrónico registrado. Por favor edite el registro y agregue un correo.");
        }

        boolean reactivado = false;
        // Si el empleado estaba RETIRADO o INACTIVO, reactivarlo para permitirle acceso
        if (empleado.getEstado() != EstadoEmpleado.ACTIVO) {
            empleado.setEstado(EstadoEmpleado.ACTIVO);
            empleado.setFechaRetiro(null);
            empleadoRepository.save(empleado);
            reactivado = true;
        }

        // Asegurar que el usuario esté activo
        usuarioRepository.findByNumeroDocumento(tercero.getNumeroDocumento())
                .ifPresent(u -> {
                    u.setActivo(true);
                    usuarioRepository.save(u);
                });

        enviarInvitacionEmpleado(empleado, tercero, null);

        if (reactivado) {
            return "Colaborador reactivado e invitación para crear contraseña enviada exitosamente a " + tercero.getEmailPrincipal();
        }
        return "Invitación para crear contraseña reenviada exitosamente a " + tercero.getEmailPrincipal();
    }

    private void enviarInvitacionEmpleado(EmpleadoEntity empleado, TerceroEntity tercero, RolUsuario rolParam) {
        try {
            RolUsuario rol = (rolParam != null) ? rolParam : mapearCargoARol(empleado.getCargo());

            UsuarioEntity usuario = usuarioRepository.findByNumeroDocumento(tercero.getNumeroDocumento())
                    .map(u -> {
                        if (rolParam != null) u.setRol(rolParam);
                        u.setActivo(true);
                        return usuarioRepository.save(u);
                    })
                    .orElseGet(() -> {
                        UsuarioEntity nuevo = UsuarioEntity.builder()
                                .tipoDocumento(tercero.getTipoDocumento() != null ? tercero.getTipoDocumento() : TipoDocumento.CC)
                                .numeroDocumento(tercero.getNumeroDocumento())
                                .nombresApellidos(tercero.getRazonSocialONombre())
                                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                                .rol(rol)
                                .activo(true)
                                .build();
                        return usuarioRepository.save(nuevo);
                    });

            // Generar token de invitación válido por 48 horas
            String tokenString = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
            TokenAutenticacionEntity tokenEntity = TokenAutenticacionEntity.builder()
                    .usuario(usuario)
                    .empleado(empleado)
                    .token(tokenString)
                    .tipo(TipoToken.INVITACION)
                    .emailDestinatario(tercero.getEmailPrincipal().trim())
                    .fechaExpiracion(OffsetDateTime.now().plusHours(48))
                    .usado(false)
                    .build();

            tokenRepository.save(tokenEntity);

            String setupUrl = (frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl)
                    + "/establecer-password?token=" + tokenString;

            String html = EmailTemplateBuilder.buildInvitacionEmpleado(
                    tercero.getRazonSocialONombre(),
                    empleado.getCargo(),
                    tercero.getNumeroDocumento(),
                    setupUrl
            );

            notificationGateway.sendEmail(
                    tercero.getEmailPrincipal().trim(),
                    "Invitación y Activación de Cuenta - CDA San Pedro",
                    html,
                    null,
                    null
            );

            log.info(">> [INVITACION_EMPLEADO] Correo de activación despachado a {} para empleado Doc: {}",
                    tercero.getEmailPrincipal(), tercero.getNumeroDocumento());
        } catch (Exception ex) {
            log.error(">> [INVITACION_EMPLEADO] Error al despachar invitación a {}: {}", 
                    tercero.getEmailPrincipal(), ex.getMessage(), ex);
        }
    }

    public RolUsuario mapearCargoARol(String cargo) {
        if (cargo == null) return RolUsuario.OPERATIVO;
        return switch (cargo.trim().toUpperCase()) {
            case "ADMINISTRADOR", "GERENTE" -> RolUsuario.ADMINISTRADOR;
            case "DIRECTOR_TECNICO", "DIRECTOR_TECNICO_SUPLENTE" -> RolUsuario.DIRECTOR_TECNICO;
            case "RECEPCIONISTA", "CAJERO", "CAJERO_RECEPCIONISTA" -> RolUsuario.RECEPCIONISTA;
            case "INSPECTOR_LINEA_LIVIANOS", "INSPECTOR_LINEA_PESADOS", "INSPECTOR_LINEA_MOTOS", "TECNICO_PISTA" -> RolUsuario.TECNICO_PISTA;
            default -> RolUsuario.OPERATIVO;
        };
    }

    @Transactional
    public CertificacionResponseDto agregarCertificacion(CertificacionRequestDto request) {
        EmpleadoEntity empleado = empleadoRepository.findById(request.getEmpleadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con ID: " + request.getEmpleadoId()));

        EmpleadoCertificacionEntity cert = EmpleadoCertificacionEntity.builder()
                .empleado(empleado)
                .tipoCertificacion(request.getTipoCertificacion())
                .codigoCertificado(request.getCodigoCertificado().trim())
                .entidadEmisora(request.getEntidadEmisora() != null ? request.getEntidadEmisora().trim() : "SENA / Organismo Acreditador")
                .fechaEmision(request.getFechaEmision())
                .fechaVencimiento(request.getFechaVencimiento())
                .soporteUrl(request.getSoporteUrl())
                .estado(request.getEstado() != null ? request.getEstado() : "VIGENTE")
                .observaciones(request.getObservaciones())
                .build();

        return mapToCertDto(certificacionRepository.save(cert));
    }

    @Transactional(readOnly = true)
    public List<CertificacionResponseDto> listarCertificaciones() {
        return certificacionRepository.findAllWithEmpleado().stream()
                .map(this::mapToCertDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void eliminarCertificacion(UUID id) {
        certificacionRepository.deleteById(id);
    }

    public EmpleadoResponseDto mapToEmpleadoDto(EmpleadoEntity e) {
        List<CertificacionResponseDto> certs = new ArrayList<>();
        if (e.getId() != null) {
            try {
                certs = certificacionRepository.findByEmpleadoIdOrderByFechaVencimientoAsc(e.getId()).stream()
                        .map(this::mapToCertDto)
                        .collect(Collectors.toList());
            } catch (Exception ex) {
                log.warn(">> [EMPLEADOS] No se pudieron cargar certificaciones para empleado ID {}: {}", e.getId(), ex.getMessage());
            }
        }

        TerceroEntity t = e.getTercero();
        RolUsuario rolApp = mapearCargoARol(e.getCargo());
        Boolean usuarioActivo = null;
        UUID usuarioId = null;

        if (t != null && t.getNumeroDocumento() != null) {
            try {
                UsuarioEntity u = usuarioRepository.findByNumeroDocumento(t.getNumeroDocumento()).orElse(null);
                if (u != null) {
                    rolApp = u.getRol();
                    usuarioActivo = u.getActivo();
                    usuarioId = u.getId();
                }
            } catch (Exception ex) {
                log.warn(">> [EMPLEADOS] No se pudo cargar usuario para doc {}: {}", t.getNumeroDocumento(), ex.getMessage());
            }
        }

        return EmpleadoResponseDto.builder()
                .id(e.getId())
                .terceroId(t != null ? t.getId() : null)
                .tipoDocumento(t != null && t.getTipoDocumento() != null ? t.getTipoDocumento().name() : "CC")
                .numeroDocumento(t != null ? t.getNumeroDocumento() : "")
                .nombresApellidos(t != null ? t.getRazonSocialONombre() : "")
                .celular(t != null ? t.getCelularPrincipal() : "")
                .email(t != null ? t.getEmailPrincipal() : "")
                .direccion(t != null ? t.getDireccion() : "")
                .cargo(e.getCargo())
                .departamento(e.getDepartamento())
                .tipoContrato(e.getTipoContrato())
                .salarioBase(e.getSalarioBase())
                .auxilioTransporteAplica(e.getAuxilioTransporteAplica())
                .banco(e.getBanco())
                .tipoCuenta(e.getTipoCuenta())
                .numeroCuenta(e.getNumeroCuenta())
                .fechaIngreso(e.getFechaIngreso())
                .fechaRetiro(e.getFechaRetiro())
                .estado(e.getEstado())
                .rolApp(rolApp)
                .usuarioActivo(usuarioActivo)
                .usuarioId(usuarioId)
                .certificaciones(certs)
                .createdAt(e.getCreatedAt())
                .build();
    }

    public CertificacionResponseDto mapToCertDto(EmpleadoCertificacionEntity c) {
        LocalDate hoy = LocalDate.now();
        long dias = (c.getFechaVencimiento() != null) 
                ? ChronoUnit.DAYS.between(hoy, c.getFechaVencimiento()) 
                : 999L;

        String color;
        String estado;
        if (dias <= 0) {
            color = "ROJO";
            estado = "VENCIDO";
        } else if (dias <= 30) {
            color = "AMARILLO";
            estado = "PROXIMO_VENCER";
        } else {
            color = "VERDE";
            estado = "VIGENTE";
        }

        EmpleadoEntity e = c.getEmpleado();
        TerceroEntity t = e != null ? e.getTercero() : null;

        return CertificacionResponseDto.builder()
                .id(c.getId())
                .empleadoId(e != null ? e.getId() : null)
                .empleadoNombre(t != null ? t.getRazonSocialONombre() : "")
                .empleadoDocumento(t != null ? t.getNumeroDocumento() : "")
                .empleadoCargo(e != null ? e.getCargo() : "")
                .tipoCertificacion(c.getTipoCertificacion())
                .codigoCertificado(c.getCodigoCertificado())
                .entidadEmisora(c.getEntidadEmisora())
                .fechaEmision(c.getFechaEmision())
                .fechaVencimiento(c.getFechaVencimiento())
                .diasRestantes(dias)
                .colorSemaforo(color)
                .soporteUrl(c.getSoporteUrl())
                .estado(estado)
                .observaciones(c.getObservaciones())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
