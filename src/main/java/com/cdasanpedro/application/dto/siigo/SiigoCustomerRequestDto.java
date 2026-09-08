package com.cdasanpedro.application.dto.siigo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiigoCustomerRequestDto {

    @JsonProperty("type")
    @Builder.Default
    private String type = "Customer";

    @JsonProperty("person_type")
    @Builder.Default
    private String personType = "Person";

    @JsonProperty("id_type")
    private String idType;

    @JsonProperty("identification")
    private String identification;

    @JsonProperty("check_digit")
    private String checkDigit;

    @JsonProperty("name")
    private List<String> name;

    @JsonProperty("commercial_name")
    private String commercialName;

    @JsonProperty("branch_office")
    @Builder.Default
    private Integer branchOffice = 0;

    @JsonProperty("active")
    @Builder.Default
    private Boolean active = true;

    @JsonProperty("vat_responsible")
    @Builder.Default
    private Boolean vatResponsible = false;

    @JsonProperty("fiscal_responsibilities")
    private List<FiscalResponsibilityDto> fiscalResponsibilities;

    @JsonProperty("address")
    private CustomerAddressDto address;

    @JsonProperty("phones")
    private List<CustomerPhoneDto> phones;

    @JsonProperty("contacts")
    private List<CustomerContactDto> contacts;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FiscalResponsibilityDto {
        @JsonProperty("code")
        private String code;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerAddressDto {
        @JsonProperty("address")
        private String address;

        @JsonProperty("city")
        private CityDto city;

        @JsonProperty("postal_code")
        private String postalCode;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CityDto {
        @JsonProperty("country_code")
        @Builder.Default
        private String countryCode = "Co";

        @JsonProperty("state_code")
        private String stateCode;

        @JsonProperty("city_code")
        private String cityCode;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerPhoneDto {
        @JsonProperty("indicative")
        @Builder.Default
        private String indicative = "57";

        @JsonProperty("number")
        private String number;

        @JsonProperty("extension")
        private String extension;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerContactDto {
        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;

        @JsonProperty("email")
        private String email;

        @JsonProperty("phone")
        private CustomerPhoneDto phone;
    }
}
