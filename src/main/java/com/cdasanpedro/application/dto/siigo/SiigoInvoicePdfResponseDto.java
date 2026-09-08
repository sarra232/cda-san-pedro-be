package com.cdasanpedro.application.dto.siigo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiigoInvoicePdfResponseDto {
    private String id;
    private String cufe;
    @JsonProperty("base64")
    private String base64;
}
