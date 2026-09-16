package com.cdasanpedro.application.dto.ingreso;

import com.cdasanpedro.core.model.enums.TipoPrueba;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechazoOrdenRequestDto {

    @NotBlank(message = "La descripción o motivo del rechazo es obligatorio")
    private String motivo;

    private String evidencia; // URL, descripción o referencia fotográfica de soporte

    @Builder.Default
    private List<TipoPrueba> pruebasRechazadas = new ArrayList<>();
}
