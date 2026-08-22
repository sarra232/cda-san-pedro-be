package com.cdasanpedro.infrastructure.persistence.entity;

import com.cdasanpedro.core.model.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 15, nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", length = 25, nullable = false, unique = true)
    private String numeroDocumento;

    @Column(name = "nombres_razon_social", length = 150, nullable = false)
    private String nombresRazonSocial;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "celular", length = 20, nullable = false)
    private String celular;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

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
