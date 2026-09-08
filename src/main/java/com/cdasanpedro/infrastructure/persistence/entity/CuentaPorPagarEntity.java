package com.cdasanpedro.infrastructure.persistence.entity;

import com.cdasanpedro.core.model.enums.EstadoCuentaPagar;
import com.cdasanpedro.core.model.enums.PeriodicidadPago;
import com.cdasanpedro.core.model.enums.TipoObligacion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cuentas_por_pagar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaPorPagarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acreedor_tercero_id", nullable = false)
    private TerceroEntity acreedor;

    @Column(name = "numero_referencia", length = 60)
    private String numeroReferencia;

    @Column(name = "concepto", length = 250, nullable = false)
    private String concepto;

    @Column(name = "monto_total", precision = 14, scale = 2, nullable = false)
    private BigDecimal montoTotal;

    @Column(name = "saldo_pendiente", precision = 14, scale = 2, nullable = false)
    private BigDecimal saldoPendiente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_obligacion", length = 40, nullable = false)
    @Builder.Default
    private TipoObligacion tipoObligacion = TipoObligacion.FACTURA_PROVEEDOR;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodicidad", length = 25, nullable = false)
    @Builder.Default
    private PeriodicidadPago periodicidad = PeriodicidadPago.PAGO_UNICO;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "dias_aviso_anticipado", nullable = false)
    @Builder.Default
    private Integer diasAvisoAnticipado = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 25, nullable = false)
    @Builder.Default
    private EstadoCuentaPagar estado = EstadoCuentaPagar.PENDIENTE;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String metadata = "{}";

    @OneToMany(mappedBy = "cuentaPorPagar", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PagoProveedorEntity> pagos = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.fechaEmision == null) {
            this.fechaEmision = LocalDate.now();
        }
        if (this.saldoPendiente == null) {
            this.saldoPendiente = this.montoTotal;
        }
        if (this.estado == null) {
            this.estado = EstadoCuentaPagar.PENDIENTE;
        }
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
