package com.example.aitourism.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CatalogPoi {
    private Long id;
    private String poiCode;
    private String region;
    private String category;
    private String nameZhHans;
    private String nameZhHant;
    private String nameEn;
    private String namePt;
    private String descriptionZhHans;
    private String descriptionZhHant;
    private String descriptionEn;
    private String descriptionPt;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String openingHours;
    private String accessibilityLevel;
    private String sourceOrganization;
    private String sourceUrl;
    private LocalDate sourcePublishedAt;
    private LocalDate validUntil;
    private Boolean naturalMerchant;
    private String status;
}
