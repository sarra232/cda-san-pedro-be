package com.cdasanpedro.application.dto.ingreso;

import com.cdasanpedro.core.model.enums.EstadoPrueba;
import com.cdasanpedro.core.model.enums.TipoPrueba;
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
public class PruebaInspeccionResponseDto {

    private UUID id;
    private UUID ordenIngresoId;
    private TipoPrueba tipoPrueba;
    private EstadoPrueba estado;
    private String observaciones;
    private UUID usuarioResponsableId;
    private String usuarioResponsableNombre;
    private String usuarioResponsableRol;
    private OffsetDateTime fechaEjecucion;
    private OffsetDateTime createdAt;
}
