package ro.autobrand.proba.dto;

import java.math.BigDecimal;

public record InvoiceLineDto(
        String productCode,
        String productName,
        BigDecimal unitPrice,
        String currency,
        BigDecimal quantity
) {
}
