package com.cdasanpedro.application.dto.siigo;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiigoStatusResponseDto {

    private boolean configured;
    private String environment;
    private String apiUrl;
    private String username;
    private Integer documentTypeId;
    private boolean connected;
    private String message;
}
