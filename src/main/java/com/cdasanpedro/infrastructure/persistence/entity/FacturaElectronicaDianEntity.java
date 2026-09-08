package com.cdasanpedro.infrastructure.persistence.entity;

import com.cdasanpedro.core.model.enums.EstadoFacturaDian;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "facturas_electronicas_dian")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaElectronicaDianEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false, unique = true)
    private FacturaEntity factura;

    @Column(name = "ambiente", length = 20, nullable = false)
    @Builder.Default
    private String ambiente = "SANDBOX";

    @Column(name = "siigo_invoice_id", length = 100)
    private String siigoInvoiceId;

    @Column(name = "numero_factura_siigo", length = 50)
    private String numeroFacturaSiigo;

    @Column(name = "cufe", length = 255)
    private String cufe;

    @Column(name = "qr_dian", columnDefinition = "text")
    private String qrDian;

    @Column(name = "pdf_siigo_url", length = 500)
    private String pdfSiigoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_dian", length = 30, nullable = false)
    @Builder.Default
    private EstadoFacturaDian estadoDian = EstadoFacturaDian.PENDIENTE;

    @Column(name = "mensaje_respuesta", columnDefinition = "text")
    private String mensajeRespuesta;

    @Column(name = "intentos", nullable = false)
    @Builder.Default
    private Integer intentos = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_enviado", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String payloadEnviado = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "respuesta_siigo", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String respuestaSiigo = "{}";

    @Column(name = "fecha_emision_dian")
    private OffsetDateTime fechaEmisionDian;

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
