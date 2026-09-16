package com.cdasanpedro.application.dto.hr;

import com.cdasanpedro.core.model.enums.EstadoEmpleado;
import com.cdasanpedro.core.model.enums.TipoContrato;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpleadoResponseDto {
    private UUID id;
    private UUID terceroId;
    private String tipoDocumento;
    private String numeroDocumento;
    private String nombresApellidos;
    private String celular;
    private String email;
    private String direccion;
    private String cargo;
    private String departamento;
    private TipoContrato tipoContrato;
    private BigDecimal salarioBase;
    private Boolean auxilioTransporteAplica;
    private String banco;
    private String tipoCuenta;
    private String numeroCuenta;
    private LocalDate fechaIngreso;
    private LocalDate fechaRetiro;
    private EstadoEmpleado estado;
    private com.cdasanpedro.core.model.enums.RolUsuario rolApp;
    private Boolean usuarioActivo;
    private UUID usuarioId;
    private List<CertificacionResponseDto> certificaciones;
    private OffsetDateTime createdAt;
}
