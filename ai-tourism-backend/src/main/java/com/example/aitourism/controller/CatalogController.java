package com.example.aitourism.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.example.aitourism.dto.BaseResponse;
import com.example.aitourism.dto.catalog.CatalogResponse;
import com.example.aitourism.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @SaIgnore
    @GetMapping("/pois")
    public BaseResponse<CatalogResponse> list(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(defaultValue = "zh-Hans") String lang
    ) {
        return BaseResponse.success(catalogService.list(region, category, keyword, lang));
    }
}
