package com.cdasanpedro.application.dto.cuentapagar;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificarCuentasRequestDto {
    private List<String> emails;
    private List<String> telefonos;
    private String mensajePersonalizado;
}
