package ro.autobrand.proba.dto;

import java.math.BigDecimal;

public record ScrapedProductDto(
        String name,
        BigDecimal price,
        String description,
        String imageUrl
) {
}
