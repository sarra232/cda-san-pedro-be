package com.cdasanpedro.application.dto.usuario;

import com.cdasanpedro.core.model.enums.RolUsuario;
import com.cdasanpedro.core.model.enums.TipoDocumento;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUsuarioDto {

    private TipoDocumento tipoDocumento;

    @Size(min = 5, max = 25, message = "El documento debe tener entre 5 y 25 caracteres")
    private String numeroDocumento;

    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    private String nombresApellidos;

    @Size(min = 6, message = "La contraseña debe tener mínimo 6 caracteres")
    private String password;

    private RolUsuario rol;

    private Boolean activo;
}
