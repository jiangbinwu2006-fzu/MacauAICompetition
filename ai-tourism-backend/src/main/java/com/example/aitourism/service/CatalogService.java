package com.example.aitourism.service;

import com.example.aitourism.dto.catalog.CatalogResponse;
import com.example.aitourism.entity.CatalogPoi;

import java.util.List;
import java.util.Optional;

public interface CatalogService {
    CatalogResponse list(String region, String category, String keyword, String language);

    List<CatalogPoi> listActiveEntities();

    Optional<CatalogPoi> findActiveById(Long id);

    Optional<CatalogPoi> findActiveByCode(String poiCode);
}
