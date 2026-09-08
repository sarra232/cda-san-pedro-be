package com.cdasanpedro.application.dto.siigo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiigoInvoiceResponseDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("number")
    private Integer number;

    @JsonProperty("date")
    private String date;

    @JsonProperty("cufe")
    private String cufe;

    @JsonProperty("qr_code")
    private String qrCode;

    @JsonProperty("public_url")
    private String publicUrl;

    @JsonProperty("total")
    private BigDecimal total;

    @JsonProperty("stamp")
    private StampDto stamp;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StampDto {
        @JsonProperty("status")
        private String status;

        @JsonProperty("cufe")
        private String cufe;

        @JsonProperty("qr")
        private String qr;
    }
}
