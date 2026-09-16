package com.cdasanpedro.infrastructure.persistence.entity;

import com.cdasanpedro.core.model.enums.EstadoNomina;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "nominas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NominaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "periodo_anio", nullable = false)
    private Integer periodoAnio;

    @Column(name = "periodo_mes", nullable = false)
    private Integer periodoMes;

    @Column(name = "periodo_quincena", nullable = false)
    @Builder.Default
    private Integer periodoQuincena = 2;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "total_devengado", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalDevengado = BigDecimal.ZERO;

    @Column(name = "total_deducciones", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalDeducciones = BigDecimal.ZERO;

    @Column(name = "total_neto", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalNeto = BigDecimal.ZERO;

    @Column(name = "total_aportes_patronales", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalAportesPatronales = BigDecimal.ZERO;

    @Column(name = "total_provisiones", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalProvisiones = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private EstadoNomina estado = EstadoNomina.BORRADOR;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_por_pagar_id")
    private CuentaPorPagarEntity cuentaPorPagar;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private String metadata = "{}";

    @OneToMany(mappedBy = "nomina", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<NominaDetalleEntity> detalles = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

