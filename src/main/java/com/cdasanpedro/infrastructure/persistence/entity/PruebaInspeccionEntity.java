package com.cdasanpedro.infrastructure.persistence.entity;

import com.cdasanpedro.core.model.enums.EstadoPrueba;
import com.cdasanpedro.core.model.enums.TipoPrueba;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pruebas_inspeccion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PruebaInspeccionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_ingreso_id", nullable = false)
    private OrdenIngresoEntity ordenIngreso;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_prueba", length = 50, nullable = false)
    private TipoPrueba tipoPrueba;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 25, nullable = false)
    @Builder.Default
    private EstadoPrueba estado = EstadoPrueba.PENDIENTE;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private UsuarioEntity usuarioResponsable;

    @Column(name = "fecha_ejecucion")
    private OffsetDateTime fechaEjecucion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoPrueba.PENDIENTE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
