package com.cdasanpedro.application.dto.siigo;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiigoSyncResultDto {

    private int totalProcesados;
    private int nuevosCreados;
    private int actualizados;
    private int fallidos;
    private String mensaje;
}
