package com.cdasanpedro.infrastructure.persistence.entity;

import com.cdasanpedro.core.model.enums.TipoToken;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tokens_autenticacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenAutenticacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private EmpleadoEntity empleado;

    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoToken tipo;

    @Column(name = "email_destinatario", nullable = false, length = 150)
    private String emailDestinatario;

    @Column(name = "fecha_expiracion", nullable = false)
    private OffsetDateTime fechaExpiracion;

    @Column(name = "usado", nullable = false)
    @Builder.Default
    private Boolean usado = false;

    @Column(name = "fecha_uso")
    private OffsetDateTime fechaUso;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        if (this.usado == null) {
            this.usado = false;
        }
    }

    public boolean isExpirado() {
        return OffsetDateTime.now().isAfter(this.fechaExpiracion);
    }

    public boolean isValido() {
        return Boolean.FALSE.equals(this.usado) && !isExpirado();
    }
}
