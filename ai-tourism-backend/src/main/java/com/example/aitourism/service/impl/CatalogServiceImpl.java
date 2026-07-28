package com.example.aitourism.service.impl;

import com.example.aitourism.dto.catalog.CatalogPoiResponse;
import com.example.aitourism.dto.catalog.CatalogResponse;
import com.example.aitourism.entity.CatalogPoi;
import com.example.aitourism.mapper.CatalogPoiMapper;
import com.example.aitourism.service.CatalogService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class CatalogServiceImpl implements CatalogService {
    private static final String DATA_VERSION = "macau-demo-2026.07";
    private final CatalogPoiMapper catalogPoiMapper;

    public CatalogServiceImpl(CatalogPoiMapper catalogPoiMapper) {
        this.catalogPoiMapper = catalogPoiMapper;
    }

    @Override
    public CatalogResponse list(String region, String category, String keyword, String language) {
        String normalizedLanguage = normalizeLanguage(language);
        List<CatalogPoiResponse> items = catalogPoiMapper.findActive(
                        trimToNull(region), trimToNull(category), trimToNull(keyword))
                .stream()
                .map(poi -> localize(poi, normalizedLanguage))
                .toList();
        return new CatalogResponse(
                items,
                items.size(),
                catalogPoiMapper.findRegions(),
                catalogPoiMapper.findCategories(),
                normalizedLanguage,
                DATA_VERSION
        );
    }

    @Override
    public List<CatalogPoi> listActiveEntities() {
        return catalogPoiMapper.findActive(null, null, null);
    }

    @Override
    public Optional<CatalogPoi> findActiveById(Long id) {
        return listActiveEntities().stream().filter(poi -> poi.getId().equals(id)).findFirst();
    }

    @Override
    public Optional<CatalogPoi> findActiveByCode(String poiCode) {
        return listActiveEntities().stream()
                .filter(poi -> poi.getPoiCode().equalsIgnoreCase(poiCode))
                .findFirst();
    }

    private CatalogPoiResponse localize(CatalogPoi poi, String language) {
        String name = switch (language) {
            case "zh-Hant" -> firstNonBlank(poi.getNameZhHant(), poi.getNameZhHans());
            case "en" -> firstNonBlank(poi.getNameEn(), poi.getNameZhHant(), poi.getNameZhHans());
            case "pt" -> firstNonBlank(poi.getNamePt(), poi.getNameZhHant(), poi.getNameZhHans());
            default -> poi.getNameZhHans();
        };
        String description = switch (language) {
            case "zh-Hant" -> firstNonBlank(poi.getDescriptionZhHant(), poi.getDescriptionZhHans());
            case "en" -> firstNonBlank(poi.getDescriptionEn(), poi.getDescriptionZhHant(), poi.getDescriptionZhHans());
            case "pt" -> firstNonBlank(poi.getDescriptionPt(), poi.getDescriptionZhHant(), poi.getDescriptionZhHans());
            default -> poi.getDescriptionZhHans();
        };
        return new CatalogPoiResponse(
                poi.getId(), poi.getPoiCode(), poi.getRegion(), poi.getCategory(), name, description,
                poi.getLongitude(), poi.getLatitude(), poi.getOpeningHours(), poi.getAccessibilityLevel(),
                poi.getSourceOrganization(), poi.getSourceUrl(), poi.getSourcePublishedAt(), poi.getValidUntil(),
                Boolean.TRUE.equals(poi.getNaturalMerchant()), language
        );
    }

    private String normalizeLanguage(String language) {
        if (language == null) return "zh-Hans";
        return switch (language.trim().toLowerCase(Locale.ROOT)) {
            case "zh-hant", "zh-tw", "zh-hk" -> "zh-Hant";
            case "en", "en-us", "en-gb" -> "en";
            case "pt", "pt-pt" -> "pt";
            default -> "zh-Hans";
        };
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }
}
