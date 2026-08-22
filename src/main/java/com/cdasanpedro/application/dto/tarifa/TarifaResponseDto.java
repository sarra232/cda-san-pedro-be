package com.cdasanpedro.application.dto.tarifa;

import com.cdasanpedro.core.model.enums.CategoriaVehiculo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarifaResponseDto {
    private UUID id;
    private String codigo;
    private CategoriaVehiculo categoria;
    private String tipoServicio;
    private String nombreServicio;
    private String descripcion;
    private BigDecimal precio;
    private BigDecimal ivaPorcentaje;
    private Boolean activo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
