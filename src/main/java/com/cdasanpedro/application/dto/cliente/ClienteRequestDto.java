package com.cdasanpedro.application.dto.cliente;

import com.cdasanpedro.core.model.enums.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRequestDto {

    @NotNull(message = "El tipo de documento es obligatorio")
    private TipoDocumento tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    private String numeroDocumento;

    @NotBlank(message = "El nombre o razón social es obligatorio")
    private String nombresRazonSocial;

    private String direccion;

    @NotBlank(message = "El número de celular es obligatorio")
    @Pattern(regexp = "^[0-9+\\s-]{7,15}$", message = "Formato de celular inválido")
    private String celular;

    @Email(message = "El correo electrónico debe tener un formato válido")
    private String email;

    private LocalDate fechaNacimiento;
}
