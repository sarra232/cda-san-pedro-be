package com.cdasanpedro.application.dto.tarifa;

import com.cdasanpedro.core.model.enums.CategoriaVehiculo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarifaUpdateRequestDto {

    private String codigo;

    private CategoriaVehiculo categoria;

    private String tipoServicio;

    @NotBlank(message = "El nombre del servicio es obligatorio")
    private String nombreServicio;

    private String descripcion;

    @NotNull(message = "El precio oficial es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
    private BigDecimal precio;

    @DecimalMin(value = "0.0", inclusive = true, message = "El porcentaje de IVA no puede ser negativo")
    private BigDecimal ivaPorcentaje;

    private Boolean activo;
}
