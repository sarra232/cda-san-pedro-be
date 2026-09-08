package com.cdasanpedro.infrastructure.persistence.entity;

import com.cdasanpedro.core.model.enums.TipoRolTercero;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "terceros_roles", uniqueConstraints = {
        @UniqueConstraint(name = "uq_tercero_rol", columnNames = {"tercero_id", "tipo_rol"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerceroRolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tercero_id", nullable = false)
    private TerceroEntity tercero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_rol", length = 35, nullable = false)
    private TipoRolTercero tipoRol;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_rol", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String metadataRol = "{}";

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
