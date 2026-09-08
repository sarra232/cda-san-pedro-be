package com.cdasanpedro.application.dto.siigo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SiigoCustomerListResponseDto {

    @JsonProperty("pagination")
    private PaginationDto pagination;

    @JsonProperty("results")
    private List<CustomerItemDto> results;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaginationDto {
        @JsonProperty("page")
        private Integer page;

        @JsonProperty("page_size")
        private Integer pageSize;

        @JsonProperty("total_results")
        private Integer totalResults;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomerItemDto {
        @JsonProperty("id")
        private String id;

        @JsonProperty("identification")
        private String identification;

        @JsonProperty("id_type")
        private Object idType;

        @JsonProperty("person_type")
        private String personType;

        @JsonProperty("name")
        private List<String> name;

        @JsonProperty("commercial_name")
        private String commercialName;

        @JsonProperty("address")
        private SiigoCustomerRequestDto.CustomerAddressDto address;

        @JsonProperty("phones")
        private List<SiigoCustomerRequestDto.CustomerPhoneDto> phones;

        @JsonProperty("contacts")
        private List<SiigoCustomerRequestDto.CustomerContactDto> contacts;
    }
}
