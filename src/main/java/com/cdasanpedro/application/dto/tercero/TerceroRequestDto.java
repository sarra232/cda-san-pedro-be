package com.cdasanpedro.application.dto.tercero;

import com.cdasanpedro.core.model.enums.TipoDocumento;
import com.cdasanpedro.core.model.enums.TipoPersona;
import com.cdasanpedro.core.model.enums.TipoRolTercero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerceroRequestDto {

    @NotNull(message = "El tipo de documento es obligatorio")
    private TipoDocumento tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    private String numeroDocumento;

    private String digitoVerificacion;

    private TipoPersona tipoPersona;

    @NotBlank(message = "La razón social o nombre es obligatorio")
    private String razonSocialONombre;

    private String primerNombre;
    private String otrosNombres;
    private String primerApellido;
    private String segundoApellido;

    @NotBlank(message = "El celular principal es obligatorio")
    private String celularPrincipal;

    private String telefonoSecundario;
    private String emailPrincipal;
    private String emailFacturacion;
    private String direccion;
    private String municipioDane;
    private String departamentoDane;
    private String responsabilidadFiscal;
    private String metadata;
    private Boolean activo;

    private List<TipoRolTercero> roles;
}
