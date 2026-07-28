package com.example.aitourism.service;

import com.example.aitourism.dto.catalog.CatalogResponse;
import com.example.aitourism.entity.CatalogPoi;
import com.example.aitourism.mapper.CatalogPoiMapper;
import com.example.aitourism.service.impl.CatalogServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogServiceImplTest {

    @Test
    void localizesPortugueseAndReturnsFilterMetadata() {
        CatalogPoiMapper mapper = mock(CatalogPoiMapper.class);
        CatalogPoi poi = new CatalogPoi();
        poi.setId(1L);
        poi.setPoiCode("RUINS_ST_PAUL");
        poi.setNameZhHans("大三巴牌坊");
        poi.setNameZhHant("大三巴牌坊");
        poi.setNameEn("Ruins of St. Paul's");
        poi.setNamePt("Ruinas de Sao Paulo");
        poi.setDescriptionZhHans("澳门历史城区地标。");
        poi.setDescriptionPt("Um marco do Centro Historico de Macau.");
        poi.setNaturalMerchant(false);
        when(mapper.findActive(null, null, null)).thenReturn(List.of(poi));
        when(mapper.findRegions()).thenReturn(List.of("PENINSULA"));
        when(mapper.findCategories()).thenReturn(List.of("ATTRACTION"));

        CatalogResponse response = new CatalogServiceImpl(mapper).list(null, null, null, "pt");

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.items().getFirst().name()).isEqualTo("Ruinas de Sao Paulo");
        assertThat(response.language()).isEqualTo("pt");
    }

    @Test
    void fallsBackFromRequestedLanguageToTraditionalThenSimplifiedChinese() {
        CatalogPoiMapper mapper = mock(CatalogPoiMapper.class);
        CatalogPoi traditionalFallback = poi(1L, "P001", "简体名称", "繁體名稱");
        CatalogPoi simplifiedFallback = poi(2L, "P002", "仅有简体", "");
        when(mapper.findActive(null, null, null)).thenReturn(List.of(traditionalFallback, simplifiedFallback));
        when(mapper.findRegions()).thenReturn(List.of("PENINSULA"));
        when(mapper.findCategories()).thenReturn(List.of("ATTRACTION"));

        CatalogResponse response = new CatalogServiceImpl(mapper).list(null, null, null, "en");

        assertThat(response.items()).extracting(item -> item.name())
                .containsExactly("繁體名稱", "仅有简体");
    }

    private CatalogPoi poi(Long id, String code, String simplifiedName, String traditionalName) {
        CatalogPoi poi = new CatalogPoi();
        poi.setId(id);
        poi.setPoiCode(code);
        poi.setNameZhHans(simplifiedName);
        poi.setNameZhHant(traditionalName);
        poi.setDescriptionZhHans("简体简介");
        poi.setDescriptionZhHant("");
        poi.setNaturalMerchant(false);
        return poi;
    }
}
