package com.cdasanpedro.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pagos_proveedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoProveedorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_por_pagar_id", nullable = false)
    private CuentaPorPagarEntity cuentaPorPagar;

    @Column(name = "monto_pagado", precision = 14, scale = 2, nullable = false)
    private BigDecimal montoPagado;

    @Column(name = "fecha_pago", nullable = false)
    private OffsetDateTime fechaPago;

    @Column(name = "metodo_pago", length = 30, nullable = false)
    @Builder.Default
    private String metodoPago = "TRANSFERENCIA";

    @Column(name = "numero_comprobante", length = 80)
    private String numeroComprobante;

    @Column(name = "soporte_url_archivo", columnDefinition = "TEXT")
    private String soporteUrlArchivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.fechaPago == null) {
            this.fechaPago = OffsetDateTime.now();
        }
        this.createdAt = OffsetDateTime.now();
    }
}
