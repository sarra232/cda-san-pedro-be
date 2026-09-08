package com.cdasanpedro.application.dto.siigo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiigoCustomerResponseDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("identification")
    private String identification;

    @JsonProperty("id_type")
    private String idType;

    @JsonProperty("name")
    private List<String> name;

    @JsonProperty("commercial_name")
    private String commercialName;

    @JsonProperty("active")
    private Boolean active;
}
