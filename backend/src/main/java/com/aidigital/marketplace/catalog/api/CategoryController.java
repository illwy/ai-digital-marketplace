package com.aidigital.marketplace.catalog.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.catalog.api.dto.CategoryView;
import com.aidigital.marketplace.catalog.application.CatalogService;
import com.aidigital.marketplace.shared.web.DataResponse;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CatalogService catalogService;

    public CategoryController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public DataResponse<List<CategoryView>> list() {
        return new DataResponse<>(catalogService.listEnabledCategories());
    }
}
