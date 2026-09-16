package com.cdasanpedro.infrastructure.persistence.entity;

import com.cdasanpedro.core.model.enums.EstadoEmpleado;
import com.cdasanpedro.core.model.enums.TipoContrato;
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
@Table(name = "empleados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpleadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tercero_id", nullable = false, unique = true)
    private TerceroEntity tercero;

    @Column(name = "cargo", nullable = false, length = 100)
    private String cargo;

    @Column(name = "departamento", nullable = false, length = 100)
    @Builder.Default
    private String departamento = "OPERACIONES_PISTA";

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contrato", nullable = false, length = 50)
    @Builder.Default
    private TipoContrato tipoContrato = TipoContrato.TERMINO_INDEFINIDO;

    @Column(name = "salario_base", nullable = false, precision = 14, scale = 2)
    private BigDecimal salarioBase;

    @Column(name = "auxilio_transporte_aplica", nullable = false)
    @Builder.Default
    private Boolean auxilioTransporteAplica = true;

    @Column(name = "banco", length = 100)
    private String banco;

    @Column(name = "tipo_cuenta", length = 30)
    @Builder.Default
    private String tipoCuenta = "AHORROS";

    @Column(name = "numero_cuenta", length = 60)
    private String numeroCuenta;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Column(name = "fecha_retiro")
    private LocalDate fechaRetiro;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private EstadoEmpleado estado = EstadoEmpleado.ACTIVO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private String metadata = "{}";

    @OneToMany(mappedBy = "empleado", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EmpleadoCertificacionEntity> certificaciones = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
