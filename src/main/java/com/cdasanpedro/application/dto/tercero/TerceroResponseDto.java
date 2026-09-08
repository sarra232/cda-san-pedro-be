package com.cdasanpedro.application.dto.tercero;

import com.cdasanpedro.core.model.enums.TipoDocumento;
import com.cdasanpedro.core.model.enums.TipoPersona;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerceroResponseDto {

    private UUID id;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private String digitoVerificacion;
    private TipoPersona tipoPersona;
    private String razonSocialONombre;
    private String primerNombre;
    private String otrosNombres;
    private String primerApellido;
    private String segundoApellido;
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
    private List<TerceroRolDto> roles;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
