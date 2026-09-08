package com.cdasanpedro.application.dto.notificacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendRealEmailRequestDto {

    @NotBlank(message = "El tipo de plantilla es obligatorio (RECORDATORIO_RTM, COMPROBANTE_PAGO, CUMPLEANOS, INSPECCION_FINALIZADA, GENERAL)")
    private String tipoPlantilla;

    @NotBlank(message = "El correo destinatario es obligatorio")
    @Email(message = "Debe ser un correo electrónico válido")
    private String destinatario;

    private String nombreCliente;
    private String placa;
    private String categoriaVehiculo;
    private LocalDate fechaVencimiento;
    private Integer diasRestantes;

    private String numeroFactura;
    private BigDecimal total;
    private String metodoPago;
    private String cufe;
    private String pdfUrl;

    private String cuponOBeneficio;
    private String certificadoRurt;
    private String mensaje;
}
