package com.cdasanpedro.infrastructure.persistence.entity;

import com.cdasanpedro.core.model.enums.CategoriaVehiculo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tarifas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarifaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "codigo", length = 50)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 50)
    private CategoriaVehiculo categoria;

    @Column(name = "tipo_servicio", length = 50)
    @Builder.Default
    private String tipoServicio = "RTM_LEGAL";

    @Column(name = "nombre_servicio", nullable = false, length = 150)
    private String nombreServicio;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "precio", nullable = false, precision = 14, scale = 2)
    private BigDecimal precio;

    @Column(name = "iva_porcentaje", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal ivaPorcentaje = BigDecimal.ZERO;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
