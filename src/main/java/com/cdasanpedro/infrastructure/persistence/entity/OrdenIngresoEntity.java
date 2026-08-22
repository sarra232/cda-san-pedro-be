package com.cdasanpedro.infrastructure.persistence.entity;

import com.cdasanpedro.core.model.enums.EstadoOrden;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ordenes_ingreso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenIngresoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "consecutivo", insertable = false, updatable = false)
    private Long consecutivo;

    @Column(name = "fecha_ingreso", nullable = false)
    private OffsetDateTime fechaIngreso;

    @Column(name = "kilometraje", nullable = false)
    private Integer kilometraje;

    @Column(name = "tipo_servicio", length = 50, nullable = false)
    private String tipoServicio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 25, nullable = false)
    private EstadoOrden estado;

    @Column(name = "conductor_es_propietario", nullable = false)
    @Builder.Default
    private Boolean conductorEsPropietario = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conductor_id")
    private ClienteEntity conductor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private VehiculoEntity vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.fechaIngreso == null) {
            this.fechaIngreso = OffsetDateTime.now();
        }
        if (this.estado == null) {
            this.estado = EstadoOrden.INGRESADO;
        }
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
