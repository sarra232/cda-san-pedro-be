package com.cdasanpedro.application.dto.hr;

import com.cdasanpedro.core.model.enums.EstadoEmpleado;
import com.cdasanpedro.core.model.enums.TipoContrato;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpleadoRequestDto {

    private UUID terceroId;

    // Si no existe tercero previo, se registran sus datos
    private String tipoDocumento;
    private String numeroDocumento;
    private String nombresApellidos;
    private String celular;
    private String email;
    private String direccion;

    @NotBlank(message = "El cargo del colaborador es obligatorio")
    private String cargo;

    private String departamento;

    private TipoContrato tipoContrato;

    @NotNull(message = "El salario base es obligatorio")
    @DecimalMin(value = "0.0", message = "El salario no puede ser negativo")
    private BigDecimal salarioBase;

    private Boolean auxilioTransporteAplica;

    private String banco;
    private String tipoCuenta;
    private String numeroCuenta;

    private LocalDate fechaIngreso;
    private LocalDate fechaRetiro;

    private EstadoEmpleado estado;
    private com.cdasanpedro.core.model.enums.RolUsuario rolApp;
    private String metadata;
}
