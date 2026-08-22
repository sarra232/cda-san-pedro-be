package com.cdasanpedro.application.dto.auth;

import com.cdasanpedro.core.model.enums.RolUsuario;
import com.cdasanpedro.core.model.enums.TipoDocumento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioPerfilDto {

    private UUID id;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private String nombresApellidos;
    private RolUsuario rol;
    private Boolean activo;
    private OffsetDateTime ultimoLogin;
}
