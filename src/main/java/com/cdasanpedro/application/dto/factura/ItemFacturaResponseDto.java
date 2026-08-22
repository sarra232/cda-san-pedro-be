package com.cdasanpedro.application.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemFacturaResponseDto {

    private UUID id;
    private String descripcion;
    private Integer cantidad;
    private BigDecimal valorUnitario;
    private BigDecimal totalItem;
}
