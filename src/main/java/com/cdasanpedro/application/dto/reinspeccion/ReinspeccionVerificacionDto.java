package com.cdasanpedro.application.dto.reinspeccion;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReinspeccionVerificacionDto {

    private boolean tieneReinspeccionGratuita;
    private UUID ordenRechazadaId;
    private Long consecutivoOrdenRechazada;
    private String placa;
    private OffsetDateTime fechaRechazo;
    private OffsetDateTime fechaLimite15Dias;
    private long diasTranscurridos;
    private long diasRestantes;
    private java.util.List<String> pruebasRechazadas;
    private String mensaje;
}
