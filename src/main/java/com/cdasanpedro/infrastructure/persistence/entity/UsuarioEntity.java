package com.cdasanpedro.infrastructure.persistence.entity;

import com.cdasanpedro.core.model.enums.RolUsuario;
import com.cdasanpedro.core.model.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 15, nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", length = 25, nullable = false, unique = true)
    private String numeroDocumento;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "nombres_apellidos", length = 150, nullable = false)
    private String nombresApellidos;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", length = 25, nullable = false)
    private RolUsuario rol;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "ultimo_login")
    private OffsetDateTime ultimoLogin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
