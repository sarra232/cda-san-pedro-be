package com.cdasanpedro.application.usecase.auth;

import com.cdasanpedro.application.dto.auth.*;
import com.cdasanpedro.application.validator.PasswordValidator;
import com.cdasanpedro.core.exception.BusinessException;
import com.cdasanpedro.core.exception.ResourceNotFoundException;
import com.cdasanpedro.core.gateway.NotificationGateway;
import com.cdasanpedro.core.model.enums.EstadoEmpleado;
import com.cdasanpedro.core.model.enums.RolUsuario;
import com.cdasanpedro.core.model.enums.TipoDocumento;
import com.cdasanpedro.core.model.enums.TipoToken;
import com.cdasanpedro.infrastructure.notification.EmailTemplateBuilder;
import com.cdasanpedro.infrastructure.persistence.entity.EmpleadoEntity;
import com.cdasanpedro.infrastructure.persistence.entity.TerceroEntity;
import com.cdasanpedro.infrastructure.persistence.entity.TokenAutenticacionEntity;
import com.cdasanpedro.infrastructure.persistence.entity.UsuarioEntity;
import com.cdasanpedro.infrastructure.persistence.repository.EmpleadoRepository;
import com.cdasanpedro.infrastructure.persistence.repository.TerceroRepository;
import com.cdasanpedro.infrastructure.persistence.repository.TokenAutenticacionRepository;
import com.cdasanpedro.infrastructure.persistence.repository.UsuarioRepository;
import com.cdasanpedro.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TerceroRepository terceroRepository;
    private final TokenAutenticacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationGateway notificationGateway;
    private final PasswordValidator passwordValidator;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        String numeroDoc = request.getNumeroDocumento().trim();

        UsuarioEntity usuario = usuarioRepository.findByNumeroDocumento(numeroDoc)
                .orElseThrow(() -> new BusinessException("Credenciales incorrectas: Número de documento o contraseña inválidos"));

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new BusinessException("El usuario se encuentra inactivo en el sistema. Contacte al administrador.");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new BusinessException("Credenciales incorrectas: Número de documento o contraseña inválidos");
        }

        // Actualizar fecha de último login
        usuario.setUltimoLogin(OffsetDateTime.now());
        usuarioRepository.save(usuario);

        String token = jwtService.generateToken(usuario);

        return LoginResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .id(usuario.getId())
                .tipoDocumento(usuario.getTipoDocumento())
                .numeroDocumento(usuario.getNumeroDocumento())
                .nombresApellidos(usuario.getNombresApellidos())
                .rol(usuario.getRol())
                .build();
    }

    @Transactional(readOnly = true)
    public UsuarioPerfilDto getPerfil(UUID usuarioId) {
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return UsuarioPerfilDto.builder()
                .id(usuario.getId())
                .tipoDocumento(usuario.getTipoDocumento())
                .numeroDocumento(usuario.getNumeroDocumento())
                .nombresApellidos(usuario.getNombresApellidos())
                .rol(usuario.getRol())
                .activo(usuario.getActivo())
                .ultimoLogin(usuario.getUltimoLogin())
                .build();
    }

    /**
     * Solicitar recuperación de contraseña vía correo electrónico.
     * Valida que el empleado/usuario exista y esté ACTIVO / HABILITADO.
     * Si no existe o no está habilitado, deniega con: "El correo o usuario no existe".
     */
    @Transactional
    public String solicitarRecuperacionPassword(SolicitudRecuperacionDto request) {
        String identificador = request.getIdentificador() != null ? request.getIdentificador().trim() : "";
        if (identificador.isBlank()) {
            throw new BusinessException("Debe proporcionar un número de documento o correo electrónico");
        }

        // 1. Buscar en Tercero o Usuario
        Optional<TerceroEntity> terceroOpt = terceroRepository.findByNumeroDocumento(identificador);
        if (terceroOpt.isEmpty() && identificador.contains("@")) {
            terceroOpt = terceroRepository.findByEmailPrincipal(identificador);
        }

        Optional<UsuarioEntity> usuarioOpt = usuarioRepository.findByNumeroDocumento(identificador);

        // Si no se encuentra ningún registro
        if (terceroOpt.isEmpty() && usuarioOpt.isEmpty()) {
            log.warn(">> [RECUPERACION_PASSWORD] Intento fallido con identificador desconocido: {}", identificador);
            throw new BusinessException("El correo o usuario no existe");
        }

        TerceroEntity tercero = terceroOpt.orElse(null);
        UsuarioEntity usuario = usuarioOpt.orElse(null);

        // Si encontramos el usuario pero no el tercero, intentar buscar tercero por documento del usuario
        if (tercero == null && usuario != null) {
            tercero = terceroRepository.findByNumeroDocumento(usuario.getNumeroDocumento()).orElse(null);
        }

        // Si encontramos el tercero pero no el usuario, intentar buscar usuario por documento del tercero
        if (usuario == null && tercero != null) {
            usuario = usuarioRepository.findByNumeroDocumento(tercero.getNumeroDocumento()).orElse(null);
        }

        // 2. Verificar si hay un Empleado asociado
        EmpleadoEntity empleado = null;
        if (tercero != null) {
            empleado = empleadoRepository.findByTerceroId(tercero.getId()).orElse(null);
        }

        // 3. REGLA ESTRICTA: Validar que el empleado esté ACTIVO/HABILITADO
        if (empleado != null && empleado.getEstado() != EstadoEmpleado.ACTIVO) {
            log.warn(">> [RECUPERACION_PASSWORD] Denegado: Empleado no activo (Estado: {}) para Doc: {}", 
                    empleado.getEstado(), tercero != null ? tercero.getNumeroDocumento() : "N/A");
            throw new BusinessException("El correo o usuario no existe");
        }

        if (usuario != null && Boolean.FALSE.equals(usuario.getActivo())) {
            log.warn(">> [RECUPERACION_PASSWORD] Denegado: Usuario inactivo para Doc: {}", usuario.getNumeroDocumento());
            throw new BusinessException("El correo o usuario no existe");
        }

        // 4. Obtener correo de destino
        String emailDestino = null;
        if (tercero != null && tercero.getEmailPrincipal() != null && !tercero.getEmailPrincipal().isBlank()) {
            emailDestino = tercero.getEmailPrincipal().trim();
        }

        if (emailDestino == null || emailDestino.isBlank()) {
            log.warn(">> [RECUPERACION_PASSWORD] No se encontró correo electrónico para el usuario/tercero");
            throw new BusinessException("El usuario no tiene un correo electrónico registrado para recuperación");
        }

        // 5. Si el usuario aún no existe pero el empleado sí es activo, crearlo
        if (usuario == null && tercero != null) {
            RolUsuario rol = (empleado != null) ? mapearCargoARol(empleado.getCargo()) : RolUsuario.OPERATIVO;
            usuario = UsuarioEntity.builder()
                    .tipoDocumento(tercero.getTipoDocumento() != null ? tercero.getTipoDocumento() : TipoDocumento.CC)
                    .numeroDocumento(tercero.getNumeroDocumento())
                    .nombresApellidos(tercero.getRazonSocialONombre())
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .rol(rol)
                    .activo(true)
                    .build();
            usuario = usuarioRepository.save(usuario);
        }

        // 6. Generar Token de Recuperación de 24 Horas
        String tokenString = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        TokenAutenticacionEntity tokenEntity = TokenAutenticacionEntity.builder()
                .usuario(usuario)
                .empleado(empleado)
                .token(tokenString)
                .tipo(TipoToken.RECUPERACION_PASSWORD)
                .emailDestinatario(emailDestino)
                .fechaExpiracion(OffsetDateTime.now().plusHours(24))
                .usado(false)
                .build();

        tokenRepository.save(tokenEntity);

        // 7. Despachar correo institucional con plantilla oficial
        String resetUrl = (frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl)
                + "/restablecer-password?token=" + tokenString;

        String nombreCompleto = usuario.getNombresApellidos();
        String html = EmailTemplateBuilder.buildRecuperacionPassword(nombreCompleto, usuario.getNumeroDocumento(), resetUrl);

        notificationGateway.sendEmail(emailDestino, "Recuperación de Contraseña - CDA San Pedro", html, null, null);
        log.info(">> [RECUPERACION_PASSWORD] Enlace de recuperación generado y enviado a {} (Doc: {})", 
                emailDestino, usuario.getNumeroDocumento());

        return "Hemos enviado un enlace de recuperación a tu correo electrónico registrado. Por favor revisa tu bandeja de entrada.";
    }

    /**
     * Valida un token de invitación o recuperación.
     * Verifica que no haya expirado, no haya sido usado y el usuario/empleado siga ACTIVO.
     */
    @Transactional(readOnly = true)
    public ValidacionTokenResponseDto validarToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("El token de seguridad no fue proporcionado");
        }

        TokenAutenticacionEntity tokenEntity = tokenRepository.findByToken(token.trim())
                .orElseThrow(() -> new BusinessException("El usuario no existe o el enlace no es válido"));

        if (Boolean.TRUE.equals(tokenEntity.getUsado())) {
            throw new BusinessException("Este enlace de seguridad ya ha sido utilizado");
        }

        if (tokenEntity.isExpirado()) {
            throw new BusinessException("El enlace de seguridad ha caducado. Por favor solicita uno nuevo.");
        }

        // Validar que el empleado / usuario continúe ACTIVO
        if (tokenEntity.getEmpleado() != null && tokenEntity.getEmpleado().getEstado() != EstadoEmpleado.ACTIVO) {
            throw new BusinessException("El usuario no existe");
        }

        if (tokenEntity.getUsuario() != null && Boolean.FALSE.equals(tokenEntity.getUsuario().getActivo())) {
            throw new BusinessException("El usuario no existe");
        }

        UsuarioEntity u = tokenEntity.getUsuario();
        return ValidacionTokenResponseDto.builder()
                .valido(true)
                .tipo(tokenEntity.getTipo().name())
                .nombresApellidos(u != null ? u.getNombresApellidos() : "")
                .numeroDocumento(u != null ? u.getNumeroDocumento() : "")
                .emailEnmascarado(enmascararEmail(tokenEntity.getEmailDestinatario()))
                .mensaje("Token válido y activo")
                .build();
    }

    /**
     * Establecer o restablecer contraseña con token de seguridad.
     * Aplica validación estricta de políticas de seguridad (mínimo 8 caracteres, 1 mayúscula, 1 especial).
     */
    @Transactional
    public String establecerPassword(RestablecerPasswordDto request) {
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new BusinessException("El token de seguridad es requerido");
        }

        TokenAutenticacionEntity tokenEntity = tokenRepository.findByToken(request.getToken().trim())
                .orElseThrow(() -> new BusinessException("El usuario no existe o el enlace no es válido"));

        if (Boolean.TRUE.equals(tokenEntity.getUsado())) {
            throw new BusinessException("Este enlace de seguridad ya ha sido utilizado");
        }

        if (tokenEntity.isExpirado()) {
            throw new BusinessException("El enlace de seguridad ha caducado. Por favor solicita uno nuevo.");
        }

        // Validar que el empleado esté ACTIVO
        if (tokenEntity.getEmpleado() != null && tokenEntity.getEmpleado().getEstado() != EstadoEmpleado.ACTIVO) {
            throw new BusinessException("El usuario no existe");
        }

        UsuarioEntity usuario = tokenEntity.getUsuario();
        if (usuario == null || Boolean.FALSE.equals(usuario.getActivo())) {
            throw new BusinessException("El usuario no existe");
        }

        // Validar complejidad de contraseña
        passwordValidator.validate(request.getNewPassword(), request.getConfirmPassword());

        // Actualizar contraseña y activar usuario
        usuario.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        usuario.setActivo(true);
        usuarioRepository.save(usuario);

        // Marcar token como consumido
        tokenEntity.setUsado(true);
        tokenEntity.setFechaUso(OffsetDateTime.now());
        tokenRepository.save(tokenEntity);

        log.info(">> [PASSWORD_SET] Contraseña actualizada exitosamente para usuario Doc: {}", usuario.getNumeroDocumento());
        return "Contraseña establecida exitosamente. Ya puedes iniciar sesión en CDA San Pedro.";
    }

    private RolUsuario mapearCargoARol(String cargo) {
        if (cargo == null) return RolUsuario.OPERATIVO;
        return switch (cargo.trim().toUpperCase()) {
            case "ADMINISTRADOR", "GERENTE" -> RolUsuario.ADMINISTRADOR;
            case "DIRECTOR_TECNICO", "DIRECTOR_TECNICO_SUPLENTE" -> RolUsuario.DIRECTOR_TECNICO;
            case "RECEPCIONISTA", "CAJERO", "CAJERO_RECEPCIONISTA" -> RolUsuario.RECEPCIONISTA;
            case "INSPECTOR_LINEA_LIVIANOS", "INSPECTOR_LINEA_PESADOS", "INSPECTOR_LINEA_MOTOS", "TECNICO_PISTA" -> RolUsuario.TECNICO_PISTA;
            default -> RolUsuario.OPERATIVO;
        };
    }

    private String enmascararEmail(String email) {
        if (email == null || !email.contains("@")) return "";
        int atIndex = email.indexOf("@");
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }
}
