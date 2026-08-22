package com.cdasanpedro.application.dto.ingreso;

import com.cdasanpedro.application.dto.cliente.ClienteRequestDto;
import com.cdasanpedro.application.dto.vehiculo.VehiculoRequestDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenIngresoRequestDto {

    // Identificación del vehículo: puede enviarse el ID existente o los datos para crear/actualizar
    private UUID vehiculoId;
    
    @NotBlank(message = "La placa del vehículo es obligatoria")
    private String placa;

    private VehiculoRequestDto vehiculoData;

    // Propietario del vehículo (para crear, vincular o actualizar)
    private UUID propietarioId;
    private ClienteRequestDto propietarioData;

    @NotNull(message = "El kilometraje es obligatorio")
    @Min(value = 0, message = "El kilometraje no puede ser negativo")
    private Integer kilometraje;

    @NotBlank(message = "El tipo de servicio es obligatorio")
    @Builder.Default
    private String tipoServicio = "RTM_LEGAL";

    @NotNull(message = "Debe especificar si el conductor es el propietario del vehículo")
    @Builder.Default
    private Boolean conductorEsPropietario = true;

    // Conductor que ingresa el vehículo (si no es el propietario)
    private UUID conductorId;
    private ClienteRequestDto conductorData;

    private String observaciones;
}
