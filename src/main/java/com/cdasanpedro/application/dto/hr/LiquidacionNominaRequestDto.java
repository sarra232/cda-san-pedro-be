package com.cdasanpedro.application.dto.hr;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiquidacionNominaRequestDto {

    @NotNull(message = "El año del periodo es obligatorio")
    private Integer periodoAnio;

    @NotNull(message = "El mes del periodo es obligatorio")
    private Integer periodoMes;

    @NotNull(message = "La quincena (1 o 2) es obligatoria")
    private Integer periodoQuincena;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String observaciones;

    // Novedades opcionales por empleado
    private List<NovedadEmpleadoDto> novedades;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NovedadEmpleadoDto {
        private UUID empleadoId;
        private Integer diasTrabajados;
        private BigDecimal horasExtras;
        private BigDecimal bonificaciones;
        private BigDecimal otrasDeducciones;
    }
}
