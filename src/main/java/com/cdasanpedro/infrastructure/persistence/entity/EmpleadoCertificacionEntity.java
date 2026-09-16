package com.cdasanpedro.infrastructure.persistence.entity;

import com.cdasanpedro.core.model.enums.TipoCertificacion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificaciones_empleado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpleadoCertificacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private EmpleadoEntity empleado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_certificacion", nullable = false, length = 100)
    private TipoCertificacion tipoCertificacion;

    @Column(name = "codigo_certificado", nullable = false, length = 100)
    private String codigoCertificado;

    @Column(name = "entidad_emisora", nullable = false, length = 150)
    @Builder.Default
    private String entidadEmisora = "SENA / Organismo Acreditador";

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "soporte_url", columnDefinition = "TEXT")
    private String soporteUrl;

    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private String estado = "VIGENTE";

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
