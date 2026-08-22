package com.cdasanpedro.application.dto.ingreso;

import com.cdasanpedro.core.model.enums.EstadoPrueba;
import com.cdasanpedro.core.model.enums.TipoPrueba;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PruebaInspeccionRequestDto {

    @NotNull(message = "El tipo de prueba es obligatorio")
    private TipoPrueba tipoPrueba;

    @NotNull(message = "El estado de la prueba es obligatorio (PENDIENTE, APROBADO, RECHAZADO)")
    private EstadoPrueba estado;

    private String observaciones;
}
