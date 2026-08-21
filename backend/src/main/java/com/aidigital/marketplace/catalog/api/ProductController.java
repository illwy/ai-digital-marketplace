package com.aidigital.marketplace.catalog.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.catalog.api.dto.ProductView;
import com.aidigital.marketplace.catalog.application.CatalogService;
import com.aidigital.marketplace.shared.web.DataResponse;
import com.aidigital.marketplace.shared.web.ListResponse;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final CatalogService catalogService;

    public ProductController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ListResponse<ProductView> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        return catalogService.listOnSale(categoryId, page, pageSize);
    }

    @GetMapping("/{id}")
    public DataResponse<ProductView> detail(@PathVariable Long id) {
        return new DataResponse<>(catalogService.getOnSale(id));
    }
}
