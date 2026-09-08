package com.cdasanpedro.infrastructure.persistence.entity;

import com.cdasanpedro.core.model.enums.TipoDocumento;
import com.cdasanpedro.core.model.enums.TipoPersona;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "terceros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerceroEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 15, nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", length = 25, nullable = false, unique = true)
    private String numeroDocumento;

    @Column(name = "digito_verificacion", length = 2)
    private String digitoVerificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_persona", length = 20, nullable = false)
    @Builder.Default
    private TipoPersona tipoPersona = TipoPersona.NATURAL;

    @Column(name = "razon_social_o_nombre", length = 200, nullable = false)
    private String razonSocialONombre;

    @Column(name = "primer_nombre", length = 60)
    private String primerNombre;

    @Column(name = "otros_nombres", length = 60)
    private String otrosNombres;

    @Column(name = "primer_apellido", length = 60)
    private String primerApellido;

    @Column(name = "segundo_apellido", length = 60)
    private String segundoApellido;

    @Column(name = "celular_principal", length = 25, nullable = false)
    private String celularPrincipal;

    @Column(name = "telefono_secundario", length = 25)
    private String telefonoSecundario;

    @Column(name = "email_principal", length = 120)
    private String emailPrincipal;

    @Column(name = "email_facturacion", length = 120)
    private String emailFacturacion;

    @Column(name = "direccion", length = 250)
    private String direccion;

    @Column(name = "municipio_dane", length = 10)
    private String municipioDane;

    @Column(name = "departamento_dane", length = 5)
    private String departamentoDane;

    @Column(name = "responsabilidad_fiscal", length = 20)
    @Builder.Default
    private String responsabilidadFiscal = "R-99-PN";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String metadata = "{}";

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @OneToMany(mappedBy = "tercero", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TerceroRolEntity> roles = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
