package com.cdasanpedro.application.dto.factura;

import com.cdasanpedro.application.dto.cliente.ClienteRequestDto;
import com.cdasanpedro.core.model.enums.MetodoPago;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturaRequestDto {

    @NotNull(message = "El ID de la orden de ingreso es obligatorio")
    private UUID ordenIngresoId;

    /**
     * Tipo de pagador:
     * - 'PROPIETARIO': Facturar al propietario oficial del vehículo.
     * - 'CONDUCTOR': Facturar a la persona que trajo el vehículo a ventanilla.
     * - 'TERCERO': Facturar a una empresa o tercero con NIT/documento diferente.
     */
    @NotNull(message = "El tipo de pagador es obligatorio (PROPIETARIO, CONDUCTOR, TERCERO)")
    @Builder.Default
    private String pagadorTipo = "PROPIETARIO";

    // Si pagadorTipo es 'TERCERO', se puede pasar el ID existente o los datos completos para registrar
    private UUID clienteFacturaId;
    private ClienteRequestDto clienteFacturaData;

    @NotNull(message = "El método de pago es obligatorio")
    @Builder.Default
    private MetodoPago metodoPago = MetodoPago.EFECTIVO;

    // Ítems de la factura (opcional; si está vacío o null, se liquida automáticamente el servicio estándar)
    @Builder.Default
    private List<ItemFacturaRequestDto> items = new ArrayList<>();
}
