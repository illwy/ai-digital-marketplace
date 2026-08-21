package com.aidigital.marketplace.catalog.api;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.catalog.api.dto.CategoryView;
import com.aidigital.marketplace.catalog.api.dto.CategoryWriteRequest;
import com.aidigital.marketplace.catalog.application.CatalogService;
import com.aidigital.marketplace.shared.web.DataResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {

    private final CatalogService catalogService;

    public AdminCategoryController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public DataResponse<List<CategoryView>> list() {
        return new DataResponse<>(catalogService.listAllCategories());
    }

    @PostMapping
    public DataResponse<CategoryView> create(@Valid @RequestBody CategoryWriteRequest request) {
        return new DataResponse<>(catalogService.createCategory(request));
    }

    @PutMapping("/{id}")
    public DataResponse<CategoryView> update(@PathVariable Long id, @Valid @RequestBody CategoryWriteRequest request) {
        return new DataResponse<>(catalogService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    public DataResponse<Void> delete(@PathVariable Long id) {
        catalogService.deleteCategory(id);
        return new DataResponse<>(null);
    }
}
