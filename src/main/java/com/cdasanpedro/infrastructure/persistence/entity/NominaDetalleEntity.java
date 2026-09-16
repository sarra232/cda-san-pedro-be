package com.cdasanpedro.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "nomina_detalles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NominaDetalleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nomina_id", nullable = false)
    private NominaEntity nomina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private EmpleadoEntity empleado;

    @Column(name = "dias_trabajados", nullable = false)
    @Builder.Default
    private Integer diasTrabajados = 15;

    @Column(name = "salario_base", nullable = false, precision = 14, scale = 2)
    private BigDecimal salarioBase;

    @Column(name = "sueldo_devengado", nullable = false, precision = 14, scale = 2)
    private BigDecimal sueldoDevengado;

    @Column(name = "auxilio_transporte", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal auxilioTransporte = BigDecimal.ZERO;

    @Column(name = "horas_extras", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal horasExtras = BigDecimal.ZERO;

    @Column(name = "bonificaciones", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal bonificaciones = BigDecimal.ZERO;

    @Column(name = "total_devengado", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDevengado;

    @Column(name = "deduccion_salud", nullable = false, precision = 14, scale = 2)
    private BigDecimal deduccionSalud;

    @Column(name = "deduccion_pension", nullable = false, precision = 14, scale = 2)
    private BigDecimal deduccionPension;

    @Column(name = "otras_deducciones", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal otrasDeducciones = BigDecimal.ZERO;

    @Column(name = "total_deducciones", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDeducciones;

    @Column(name = "neto_pagar", nullable = false, precision = 14, scale = 2)
    private BigDecimal netoPagar;

    @Column(name = "aporte_salud_patronal", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal aporteSaludPatronal = BigDecimal.ZERO;

    @Column(name = "aporte_pension_patronal", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal aportePensionPatronal = BigDecimal.ZERO;

    @Column(name = "aporte_arl", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal aporteArl = BigDecimal.ZERO;

    @Column(name = "parafiscales_caja", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal parafiscalesCaja = BigDecimal.ZERO;

    @Column(name = "provision_prima", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal provisionPrima = BigDecimal.ZERO;

    @Column(name = "provision_cesantias", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal provisionCesantias = BigDecimal.ZERO;

    @Column(name = "provision_intereses_cesantias", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal provisionInteresesCesantias = BigDecimal.ZERO;

    @Column(name = "provision_vacaciones", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal provisionVacaciones = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
