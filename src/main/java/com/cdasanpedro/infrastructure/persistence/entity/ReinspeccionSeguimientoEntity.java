package com.cdasanpedro.infrastructure.persistence.entity;

import com.cdasanpedro.core.model.enums.EstadoReinspeccion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reinspeccion_seguimiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReinspeccionSeguimientoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_rechazada_id", nullable = false, unique = true)
    private OrdenIngresoEntity ordenRechazada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private VehiculoEntity vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private ClienteEntity cliente;

    @Column(name = "fecha_rechazo", nullable = false)
    private OffsetDateTime fechaRechazo;

    @Column(name = "fecha_limite_15_dias", nullable = false)
    private OffsetDateTime fechaLimite15Dias;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_seguimiento", length = 30, nullable = false)
    @Builder.Default
    private EstadoReinspeccion estadoSeguimiento = EstadoReinspeccion.EN_PLAZO;

    @Column(name = "reinspeccion_completada", nullable = false)
    @Builder.Default
    private Boolean reinspeccionCompletada = false;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_reinspeccion_id")
    private OrdenIngresoEntity ordenReinspeccion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.fechaRechazo == null) {
            this.fechaRechazo = OffsetDateTime.now();
        }
        if (this.fechaLimite15Dias == null) {
            this.fechaLimite15Dias = this.fechaRechazo.plusDays(15);
        }
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
