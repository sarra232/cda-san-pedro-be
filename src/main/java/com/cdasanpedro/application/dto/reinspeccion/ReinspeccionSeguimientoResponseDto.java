package com.cdasanpedro.application.dto.reinspeccion;

import com.cdasanpedro.core.model.enums.EstadoReinspeccion;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReinspeccionSeguimientoResponseDto {

    private UUID id;
    private UUID ordenRechazadaId;
    private Long consecutivoOrdenRechazada;
    private UUID vehiculoId;
    private String vehiculoPlaca;
    private String vehiculoMarca;
    private String vehiculoLinea;
    private String clienteNombre;
    private String clienteCelular;
    private String clienteEmail;
    private OffsetDateTime fechaRechazo;
    private OffsetDateTime fechaLimite15Dias;
    private long diasRestantes;
    private EstadoReinspeccion estadoSeguimiento;
    private Boolean reinspeccionCompletada;
    private UUID ordenReinspeccionId;
    private Long consecutivoOrdenReinspeccion;
    private String metadata;
    private OffsetDateTime createdAt;
}
