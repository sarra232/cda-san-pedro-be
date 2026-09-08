package com.cdasanpedro.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "mapeo_catalogo_siigo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MapeoCatalogoSiigoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "categoria_cda", length = 30, nullable = false, unique = true)
    private String categoriaCda;

    @Column(name = "codigo_producto_siigo", length = 50, nullable = false)
    private String codigoProductoSiigo;

    @Column(name = "descripcion_siigo", length = 200, nullable = false)
    private String descripcionSiigo;

    @Column(name = "tarifa_iva", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal tarifaIva = new BigDecimal("19.00");

    @Column(name = "siigo_tax_id")
    private Integer siigoTaxId;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

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
