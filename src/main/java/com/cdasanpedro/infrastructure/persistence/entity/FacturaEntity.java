package com.cdasanpedro.infrastructure.persistence.entity;

import com.cdasanpedro.core.model.enums.EstadoFactura;
import com.cdasanpedro.core.model.enums.MetodoPago;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "facturas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "numero_factura", length = 30, nullable = false, unique = true)
    private String numeroFactura;

    @Column(name = "consecutivo", insertable = false, updatable = false)
    private Long consecutivo;

    @Column(name = "fecha_emision", nullable = false)
    private OffsetDateTime fechaEmision;

    @Column(name = "subtotal", precision = 14, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "iva", precision = 14, scale = 2, nullable = false)
    private BigDecimal iva;

    @Column(name = "total", precision = 14, scale = 2, nullable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 25, nullable = false)
    private MetodoPago metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoFactura estado;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_ingreso_id", nullable = false)
    private OrdenIngresoEntity ordenIngreso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_factura_id", nullable = false)
    private ClienteEntity clienteFactura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemFacturaEntity> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.fechaEmision == null) {
            this.fechaEmision = OffsetDateTime.now();
        }
        if (this.estado == null) {
            this.estado = EstadoFactura.PAGADA;
        }
        this.createdAt = OffsetDateTime.now();
    }

    public void addItem(ItemFacturaEntity item) {
        items.add(item);
        item.setFactura(this);
    }
}
