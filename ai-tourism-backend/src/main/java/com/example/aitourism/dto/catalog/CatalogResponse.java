package com.example.aitourism.dto.catalog;

import java.util.List;

public record CatalogResponse(
        List<CatalogPoiResponse> items,
        int total,
        List<String> regions,
        List<String> categories,
        String language,
        String dataVersion
) {
}
