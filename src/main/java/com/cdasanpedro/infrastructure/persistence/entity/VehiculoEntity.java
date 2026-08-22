package com.cdasanpedro.infrastructure.persistence.entity;

import com.cdasanpedro.core.model.enums.CategoriaVehiculo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehiculos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiculoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "placa", length = 10, nullable = false, unique = true)
    private String placa;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", length = 20, nullable = false)
    private CategoriaVehiculo categoria;

    @Column(name = "marca", length = 60, nullable = false)
    private String marca;

    @Column(name = "linea", length = 60, nullable = false)
    private String linea;

    @Column(name = "modelo", nullable = false)
    private Integer modelo;

    @Column(name = "chasis_vin", length = 50)
    private String chasisVin;

    @Column(name = "fecha_vencimiento_soat")
    private LocalDate fechaVencimientoSoat;

    @Column(name = "fecha_vencimiento_rtm")
    private LocalDate fechaVencimientoRtm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_id")
    private ClienteEntity propietario;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.placa != null) {
            this.placa = this.placa.trim().toUpperCase();
        }
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.placa != null) {
            this.placa = this.placa.trim().toUpperCase();
        }
        this.updatedAt = OffsetDateTime.now();
    }
}
