package com.cdasanpedro.application.dto.ingreso;

import com.cdasanpedro.application.dto.cliente.ClienteResponseDto;
import com.cdasanpedro.application.dto.vehiculo.VehiculoResponseDto;
import com.cdasanpedro.core.model.enums.EstadoOrden;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenIngresoResponseDto {

    private UUID id;
    private Long consecutivo;
    private OffsetDateTime fechaIngreso;
    private Integer kilometraje;
    private String tipoServicio;
    private EstadoOrden estado;
    private Boolean conductorEsPropietario;
    private UUID ordenPadreId;
    private Long consecutivoOrdenPadre;
    private Boolean esReinspeccion;
    private Integer diasTranscurridosRechazo;
    private Long diasRestantesReinspeccion;
    private Boolean esReinspeccionVigente;
    private OffsetDateTime fechaLimiteReinspeccion;
    private java.util.List<String> pruebasRechazadasPrevias;
    private String motivoRechazo;
    private String evidenciaRechazo;
    private java.util.List<String> pruebasRechazadas;
    private VehiculoResponseDto vehiculo;
    private ClienteResponseDto conductor;
    private String usuarioNombre;
    private String observaciones;
    private Boolean facturado;
    private UUID facturaId;
    private String numeroFactura;
    private OffsetDateTime createdAt;
}
