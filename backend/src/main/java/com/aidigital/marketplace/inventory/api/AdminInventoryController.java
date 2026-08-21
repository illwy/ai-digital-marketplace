package com.aidigital.marketplace.inventory.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.catalog.application.CatalogService;
import com.aidigital.marketplace.inventory.api.dto.InventoryImportRequest;
import com.aidigital.marketplace.inventory.api.dto.InventoryView;
import com.aidigital.marketplace.inventory.application.InventoryService;
import com.aidigital.marketplace.shared.web.DataResponse;
import com.aidigital.marketplace.shared.web.ListResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminInventoryController {

    private final InventoryService inventoryService;
    private final CatalogService catalogService;

    public AdminInventoryController(InventoryService inventoryService, CatalogService catalogService) {
        this.inventoryService = inventoryService;
        this.catalogService = catalogService;
    }

    @GetMapping("/inventory-items")
    public ListResponse<InventoryView> list(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return inventoryService.list(productId, status, page, pageSize);
    }

    @PostMapping("/inventory-items")
    public DataResponse<List<InventoryView>> importItems(@Valid @RequestBody InventoryImportRequest request) {
        catalogService.getAdmin(request.productId());
        return new DataResponse<>(inventoryService.importItems(request));
    }

    @PostMapping("/inventory-items/{id}/invalidate")
    public DataResponse<InventoryView> invalidate(@PathVariable Long id) {
        return new DataResponse<>(inventoryService.invalidate(id));
    }
}
