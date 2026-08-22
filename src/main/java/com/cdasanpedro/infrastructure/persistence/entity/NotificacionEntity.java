package com.cdasanpedro.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notificaciones_cola")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private ClienteEntity cliente;

    @Column(name = "tipo", length = 30, nullable = false)
    private String tipo;

    @Column(name = "canal", length = 20, nullable = false)
    private String canal;

    @Column(name = "destinatario", length = 120, nullable = false)
    private String destinatario;

    @Column(name = "asunto", length = 200)
    private String asunto;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cuerpo_payload", columnDefinition = "jsonb", nullable = false)
    private String cuerpoPayload;

    @Column(name = "estado", length = 20, nullable = false)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(name = "intentos", nullable = false)
    @Builder.Default
    private Integer intentos = 0;

    @Column(name = "fecha_programada", nullable = false)
    private OffsetDateTime fechaProgramada;

    @Column(name = "fecha_enviado")
    private OffsetDateTime fechaEnviado;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.fechaProgramada == null) {
            this.fechaProgramada = OffsetDateTime.now();
        }
        this.createdAt = OffsetDateTime.now();
    }
}
