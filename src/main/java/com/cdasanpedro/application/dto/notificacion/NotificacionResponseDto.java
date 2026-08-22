package com.cdasanpedro.application.dto.notificacion;

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
public class NotificacionResponseDto {

    private UUID id;
    private String tipo;
    private String canal;
    private String destinatario;
    private String asunto;
    private String cuerpoPayload;
    private String estado;
    private Integer intentos;
    private OffsetDateTime fechaProgramada;
    private OffsetDateTime fechaEnviado;
    private String clienteNombre;
    private OffsetDateTime createdAt;
}
