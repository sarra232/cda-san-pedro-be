package com.cdasanpedro.application.dto.cuentapagar;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemaforoVencimientosDto {

    private long totalVencidas;
    private BigDecimal saldoVencido;

    private long totalProximas;
    private BigDecimal saldoProximo;

    private long totalAlDia;
    private BigDecimal saldoAlDia;

    private List<CuentaPorPagarResponseDto> cuentasUrgentes;
}
