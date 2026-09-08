package com.cdasanpedro.application.dto.cuentapagar;

import com.cdasanpedro.core.model.enums.EstadoCuentaPagar;
import com.cdasanpedro.core.model.enums.PeriodicidadPago;
import com.cdasanpedro.core.model.enums.TipoObligacion;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaPorPagarResponseDto {

    private UUID id;
    private UUID acreedorTerceroId;
    private String acreedorDocumento;
    private String acreedorNombre;
    private String acreedorCelular;
    private String numeroReferencia;
    private String concepto;
    private BigDecimal montoTotal;
    private BigDecimal saldoPendiente;
    private TipoObligacion tipoObligacion;
    private PeriodicidadPago periodicidad;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private Integer diasAvisoAnticipado;
    private Long diasRestantes;
    private String colorSemaforo; // ROJO, AMARILLO, VERDE, GRIS
    private EstadoCuentaPagar estado;
    private String observaciones;
    private String metadata;
    private List<PagoProveedorResponseDto> pagos;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
