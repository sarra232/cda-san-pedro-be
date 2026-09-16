package com.cdasanpedro.infrastructure.bootstrap;

import com.cdasanpedro.core.model.enums.*;
import com.cdasanpedro.infrastructure.persistence.entity.*;
import com.cdasanpedro.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final TerceroRepository terceroRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TerceroRolRepository terceroRolRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Seed Administrador inicial
        seedUsuarioYEmpleado(
                "123456789",
                TipoDocumento.CC,
                "Administrador Principal CDA",
                "admin@cdasanpedro.com",
                "3101234567",
                "Admin123*",
                RolUsuario.ADMINISTRADOR,
                "ADMINISTRADOR",
                "ADMINISTRACION",
                new BigDecimal("4500000.00"),
                false
        );

        // 2. Seed Recepcionista inicial
        seedUsuarioYEmpleado(
                "987654321",
                TipoDocumento.CC,
                "Jacinta Recepción",
                "recepcion@cdasanpedro.com",
                "3159876543",
                "Recepcion123*",
                RolUsuario.RECEPCIONISTA,
                "RECEPCIONISTA",
                "RECEPCION_Y_CAJA",
                new BigDecimal("1600000.00"),
                true
        );

        // 3. Seed Director Técnico inicial
        seedUsuarioYEmpleado(
                "100200300",
                TipoDocumento.CC,
                "Director Técnico Certificador CDA",
                "director.tecnico@cdasanpedro.com",
                "3112003004",
                "Director123*",
                RolUsuario.DIRECTOR_TECNICO,
                "DIRECTOR_TECNICO",
                "DIRECCION_TECNICA",
                new BigDecimal("4000000.00"),
                false
        );

        // 4. Seed Técnico de Pista inicial
        seedUsuarioYEmpleado(
                "1020304050",
                TipoDocumento.CC,
                "Técnico Inspector de Pista 1",
                "andres.gomez@cdasanpedro.com",
                "3128899001",
                "Tecnico123*",
                RolUsuario.TECNICO_PISTA,
                "INSPECTOR_LINEA_LIVIANOS",
                "OPERACIONES_PISTA",
                new BigDecimal("1850000.00"),
                true
        );
    }

    private void seedUsuarioYEmpleado(
            String doc,
            TipoDocumento tipoDoc,
            String nombre,
            String email,
            String celular,
            String passwordPlano,
            RolUsuario rolApp,
            String cargo,
            String departamento,
            BigDecimal salario,
            boolean auxilioTrans
    ) {
        // A. Asegurar Usuario
        UsuarioEntity usuario = usuarioRepository.findByNumeroDocumento(doc).orElse(null);
        if (usuario == null) {
            usuario = UsuarioEntity.builder()
                    .tipoDocumento(tipoDoc)
                    .numeroDocumento(doc)
                    .nombresApellidos(nombre)
                    .passwordHash(passwordEncoder.encode(passwordPlano))
                    .rol(rolApp)
                    .activo(true)
                    .build();
            usuarioRepository.save(usuario);
            log.info(">> [BOOTSTRAP] Usuario inicial creado (Doc: {} / Pass: {})", doc, passwordPlano);
        } else {
            usuario.setPasswordHash(passwordEncoder.encode(passwordPlano));
            usuario.setRol(rolApp);
            usuario.setActivo(true);
            usuarioRepository.save(usuario);
        }

        // B. Asegurar Tercero
        TerceroEntity tercero = terceroRepository.findByNumeroDocumento(doc).orElse(null);
        if (tercero == null) {
            tercero = TerceroEntity.builder()
                    .tipoDocumento(tipoDoc)
                    .numeroDocumento(doc)
                    .tipoPersona(TipoPersona.NATURAL)
                    .razonSocialONombre(nombre)
                    .emailPrincipal(email)
                    .celularPrincipal(celular)
                    .activo(true)
                    .build();
            tercero = terceroRepository.save(tercero);
        }

        // C. Asegurar Rol EMPLEADO en TerceroRol
        if (!terceroRolRepository.existsByTerceroIdAndTipoRol(tercero.getId(), TipoRolTercero.EMPLEADO)) {
            TerceroRolEntity rolEntity = TerceroRolEntity.builder()
                    .tercero(tercero)
                    .tipoRol(TipoRolTercero.EMPLEADO)
                    .activo(true)
                    .build();
            terceroRolRepository.save(rolEntity);
        }

        // D. Asegurar EmpleadoEntity
        if (empleadoRepository.findByTerceroId(tercero.getId()).isEmpty()) {
            EmpleadoEntity emp = EmpleadoEntity.builder()
                    .tercero(tercero)
                    .cargo(cargo)
                    .departamento(departamento)
                    .tipoContrato(TipoContrato.TERMINO_INDEFINIDO)
                    .salarioBase(salario)
                    .auxilioTransporteAplica(auxilioTrans)
                    .banco("Bancolombia")
                    .tipoCuenta("AHORROS")
                    .numeroCuenta("102-000000-00")
                    .fechaIngreso(LocalDate.of(2024, 1, 15))
                    .estado(EstadoEmpleado.ACTIVO)
                    .build();
            empleadoRepository.save(emp);
            log.info(">> [BOOTSTRAP] Colaborador inicial sincronizado en Talento Humano: {}", nombre);
        }
    }
}

