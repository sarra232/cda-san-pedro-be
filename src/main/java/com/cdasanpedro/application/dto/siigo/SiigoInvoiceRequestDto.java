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
public class SiigoInvoiceRequestDto {

    @JsonProperty("document")
    private DocumentRefDto document;

    @JsonProperty("date")
    private String date;

    @JsonProperty("customer")
    private CustomerRefDto customer;

    @JsonProperty("seller")
    private Integer seller;

    @JsonProperty("cost_center")
    private Integer costCenter;

    @JsonProperty("items")
    private List<InvoiceItemDto> items;

    @JsonProperty("payments")
    private List<InvoicePaymentDto> payments;

    @JsonProperty("observations")
    private String observations;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentRefDto {
        @JsonProperty("id")
        private Integer id;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerRefDto {
        @JsonProperty("identification")
        private String identification;

        @JsonProperty("branch_office")
        @Builder.Default
        private Integer branchOffice = 0;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoiceItemDto {
        @JsonProperty("code")
        private String code;

        @JsonProperty("description")
        private String description;

        @JsonProperty("quantity")
        @Builder.Default
        private Integer quantity = 1;

        @JsonProperty("price")
        private BigDecimal price;

        @JsonProperty("discount")
        @Builder.Default
        private BigDecimal discount = BigDecimal.ZERO;

        @JsonProperty("taxes")
        private List<InvoiceTaxDto> taxes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoiceTaxDto {
        @JsonProperty("id")
        private Integer id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("type")
        private String type;

        @JsonProperty("percentage")
        private BigDecimal percentage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoicePaymentDto {
        @JsonProperty("id")
        private Integer id;

        @JsonProperty("value")
        private BigDecimal value;

        @JsonProperty("due_date")
        private String dueDate;
    }
}
