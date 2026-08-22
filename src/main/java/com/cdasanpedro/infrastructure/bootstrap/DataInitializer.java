package com.cdasanpedro.infrastructure.bootstrap;

import com.cdasanpedro.core.model.enums.RolUsuario;
import com.cdasanpedro.core.model.enums.TipoDocumento;
import com.cdasanpedro.infrastructure.persistence.entity.UsuarioEntity;
import com.cdasanpedro.infrastructure.persistence.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 1. Seed Administrador inicial
        if (!usuarioRepository.existsByNumeroDocumento("123456789")) {
            UsuarioEntity admin = UsuarioEntity.builder()
                    .tipoDocumento(TipoDocumento.CC)
                    .numeroDocumento("123456789")
                    .nombresApellidos("Administrador Principal CDA")
                    .passwordHash(passwordEncoder.encode("Admin123*"))
                    .rol(RolUsuario.ADMINISTRADOR)
                    .activo(true)
                    .build();
            usuarioRepository.save(admin);
            log.info(">> [BOOTSTRAP] Usuario Administrador inicial creado (Doc: 123456789 / Pass: Admin123*)");
        } else {
            // Asegurar que la contraseña coincida con el hash
            UsuarioEntity admin = usuarioRepository.findByNumeroDocumento("123456789").orElse(null);
            if (admin != null) {
                admin.setPasswordHash(passwordEncoder.encode("Admin123*"));
                usuarioRepository.save(admin);
            }
        }

        // 2. Seed Recepcionista inicial
        if (!usuarioRepository.existsByNumeroDocumento("987654321")) {
            UsuarioEntity recepcionista = UsuarioEntity.builder()
                    .tipoDocumento(TipoDocumento.CC)
                    .numeroDocumento("987654321")
                    .nombresApellidos("Recepcionista Ventanilla 1")
                    .passwordHash(passwordEncoder.encode("Recepcion123*"))
                    .rol(RolUsuario.RECEPCIONISTA)
                    .activo(true)
                    .build();
            usuarioRepository.save(recepcionista);
            log.info(">> [BOOTSTRAP] Usuario Recepcionista inicial creado (Doc: 987654321 / Pass: Recepcion123*)");
        }

        // 3. Seed Director Técnico inicial
        if (!usuarioRepository.existsByNumeroDocumento("100200300")) {
            UsuarioEntity director = UsuarioEntity.builder()
                    .tipoDocumento(TipoDocumento.CC)
                    .numeroDocumento("100200300")
                    .nombresApellidos("Director Técnico Certificador CDA")
                    .passwordHash(passwordEncoder.encode("Director123*"))
                    .rol(RolUsuario.DIRECTOR_TECNICO)
                    .activo(true)
                    .build();
            usuarioRepository.save(director);
            log.info(">> [BOOTSTRAP] Usuario Director Técnico inicial creado (Doc: 100200300 / Pass: Director123*)");
        }

        // 4. Seed Técnico de Pista inicial
        if (!usuarioRepository.existsByNumeroDocumento("1020304050")) {
            UsuarioEntity tecnico = UsuarioEntity.builder()
                    .tipoDocumento(TipoDocumento.CC)
                    .numeroDocumento("1020304050")
                    .nombresApellidos("Técnico Inspector de Pista 1")
                    .passwordHash(passwordEncoder.encode("Tecnico123*"))
                    .rol(RolUsuario.TECNICO_PISTA)
                    .activo(true)
                    .build();
            usuarioRepository.save(tecnico);
            log.info(">> [BOOTSTRAP] Usuario Técnico de Pista inicial creado (Doc: 1020304050 / Pass: Tecnico123*)");
        }
    }
}
