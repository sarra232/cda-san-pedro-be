package com.cdasanpedro.application.dto.siigo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiigoAuthRequestDto {

    @JsonProperty("username")
    private String username;

    @JsonProperty("access_key")
    private String accessKey;
}
