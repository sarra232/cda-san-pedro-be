package com.cdasanpedro.application.dto.factura;

import com.cdasanpedro.application.dto.cliente.ClienteResponseDto;
import com.cdasanpedro.application.dto.ingreso.OrdenIngresoResponseDto;
import com.cdasanpedro.core.model.enums.EstadoFactura;
import com.cdasanpedro.core.model.enums.MetodoPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaResponseDto {

    private UUID id;
    private String numeroFactura;
    private Long consecutivo;
    private OffsetDateTime fechaEmision;
    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal total;
    private MetodoPago metodoPago;
    private EstadoFactura estado;
    private ClienteResponseDto clienteFactura;
    private OrdenIngresoResponseDto ordenIngreso;
    private String usuarioNombre;
    @Builder.Default
    private List<ItemFacturaResponseDto> items = new ArrayList<>();
    private OffsetDateTime createdAt;

    // Campos de Integración Fiscal SIIGO / DIAN
    private com.cdasanpedro.core.model.enums.EstadoFacturaDian estadoDian;
    private String numeroFacturaSiigo;
    private String pdfSiigoUrl;
    private String cufe;
    private String mensajeRespuestaDian;
}
