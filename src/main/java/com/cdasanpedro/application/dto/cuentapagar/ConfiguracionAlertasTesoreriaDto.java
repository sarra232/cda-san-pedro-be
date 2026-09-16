package com.cdasanpedro.application.dto.cuentapagar;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionAlertasTesoreriaDto {
    private List<String> emails;
    private List<String> telefonos;
    private Boolean activo;
    private String horaEnvio;
    private String cronExpression;
}
