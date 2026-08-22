package com.cdasanpedro.application.dto.factura;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemFacturaRequestDto {

    @NotBlank(message = "La descripción del ítem es obligatoria")
    private String descripcion;

    @NotNull(message = "La cantidad es obligatoria")
    @Builder.Default
    private Integer cantidad = 1;

    @NotNull(message = "El valor unitario es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El valor unitario debe ser mayor a 0")
    private BigDecimal valorUnitario;
}
