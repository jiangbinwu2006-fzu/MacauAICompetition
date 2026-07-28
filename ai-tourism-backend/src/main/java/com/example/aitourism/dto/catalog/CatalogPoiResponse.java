package com.example.aitourism.dto.catalog;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CatalogPoiResponse(
        Long id,
        String poiCode,
        String region,
        String category,
        String name,
        String description,
        BigDecimal longitude,
        BigDecimal latitude,
        String openingHours,
        String accessibilityLevel,
        String sourceOrganization,
        String sourceUrl,
        LocalDate sourcePublishedAt,
        LocalDate validUntil,
        boolean naturalMerchant,
        String language
) {
}
