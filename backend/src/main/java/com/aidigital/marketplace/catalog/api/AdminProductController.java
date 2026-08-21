package com.aidigital.marketplace.catalog.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.catalog.api.dto.ProductView;
import com.aidigital.marketplace.catalog.api.dto.ProductWriteRequest;
import com.aidigital.marketplace.catalog.application.CatalogService;
import com.aidigital.marketplace.inventory.api.dto.InventoryStatsView;
import com.aidigital.marketplace.inventory.application.InventoryService;
import com.aidigital.marketplace.shared.web.DataResponse;
import com.aidigital.marketplace.shared.web.ListResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final CatalogService catalogService;
    private final InventoryService inventoryService;

    public AdminProductController(CatalogService catalogService, InventoryService inventoryService) {
        this.catalogService = catalogService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ListResponse<ProductView> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return catalogService.listAdmin(categoryId, status, page, pageSize);
    }

    @GetMapping("/{id}")
    public DataResponse<ProductView> detail(@PathVariable Long id) {
        return new DataResponse<>(catalogService.getAdmin(id));
    }

    @PostMapping
    public DataResponse<ProductView> create(@Valid @RequestBody ProductWriteRequest request) {
        return new DataResponse<>(catalogService.createProduct(request));
    }

    @PutMapping("/{id}")
    public DataResponse<ProductView> update(@PathVariable Long id, @Valid @RequestBody ProductWriteRequest request) {
        return new DataResponse<>(catalogService.updateProduct(id, request));
    }

    @GetMapping("/{id}/inventory-stats")
    public DataResponse<InventoryStatsView> stats(@PathVariable Long id) {
        catalogService.getAdmin(id);
        return new DataResponse<>(inventoryService.stats(id));
    }
}
