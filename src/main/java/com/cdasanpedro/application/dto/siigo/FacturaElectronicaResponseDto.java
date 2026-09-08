package com.cdasanpedro.application.dto.siigo;

import com.cdasanpedro.core.model.enums.EstadoFacturaDian;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaElectronicaResponseDto {

    private UUID id;
    private UUID facturaId;
    private String numeroFacturaLocal;
    private String ambiente;
    private String siigoInvoiceId;
    private String numeroFacturaSiigo;
    private String cufe;
    private String qrDian;
    private String pdfSiigoUrl;
    private EstadoFacturaDian estadoDian;
    private String mensajeRespuesta;
    private Integer intentos;
    private OffsetDateTime fechaEmisionDian;
    private OffsetDateTime createdAt;
}
