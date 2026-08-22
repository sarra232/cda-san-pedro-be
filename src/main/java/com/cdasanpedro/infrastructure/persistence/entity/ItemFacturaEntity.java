package com.cdasanpedro.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "items_factura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemFacturaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private FacturaEntity factura;

    @Column(name = "descripcion", length = 200, nullable = false)
    private String descripcion;

    @Column(name = "cantidad", nullable = false)
    @Builder.Default
    private Integer cantidad = 1;

    @Column(name = "valor_unitario", precision = 14, scale = 2, nullable = false)
    private BigDecimal valorUnitario;

    @Column(name = "total_item", precision = 14, scale = 2, nullable = false)
    private BigDecimal totalItem;
}
