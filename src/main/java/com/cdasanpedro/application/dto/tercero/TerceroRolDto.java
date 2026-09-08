package com.cdasanpedro.application.dto.tercero;

import com.cdasanpedro.core.model.enums.TipoRolTercero;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerceroRolDto {
    private UUID id;
    private TipoRolTercero tipoRol;
    private String metadataRol;
    private Boolean activo;
}
