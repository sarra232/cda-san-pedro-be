package com.cdasanpedro.application.dto.vehiculo;

import com.cdasanpedro.core.model.enums.CategoriaVehiculo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiculoRequestDto {

    @NotBlank(message = "La placa es obligatoria")
    @Pattern(regexp = "^[A-Za-z0-9]{5,7}$", message = "Formato de placa inválido (debe tener entre 5 y 7 caracteres alfanuméricos)")
    private String placa;

    @NotNull(message = "La categoría del vehículo es obligatoria")
    private CategoriaVehiculo categoria;

    @NotBlank(message = "La marca es obligatoria")
    private String marca;

    @NotBlank(message = "La línea es obligatoria")
    private String linea;

    @NotNull(message = "El año del modelo es obligatorio")
    private Integer modelo;

    private String chasisVin;

    private LocalDate fechaVencimientoSoat;

    private LocalDate fechaVencimientoRtm;

    private UUID propietarioId;
}
