package com.aidigital.marketplace.catalog.application;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.aidigital.marketplace.catalog.api.dto.CategoryWriteRequest;
import com.aidigital.marketplace.catalog.api.dto.ProductWriteRequest;
import com.aidigital.marketplace.catalog.infrastructure.mapper.ProductMapper;
import com.aidigital.marketplace.inventory.api.dto.InventoryImportRequest;
import com.aidigital.marketplace.inventory.application.InventoryService;

@Component
@Order(20)
@ConditionalOnProperty(name = "app.demo-seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoCatalogSeed implements ApplicationRunner {

    private final ProductMapper productMapper;
    private final CatalogService catalogService;
    private final InventoryService inventoryService;

    public DemoCatalogSeed(
            ProductMapper productMapper, CatalogService catalogService, InventoryService inventoryService) {
        this.productMapper = productMapper;
        this.catalogService = catalogService;
        this.inventoryService = inventoryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long count = productMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        var category = catalogService.createCategory(new CategoryWriteRequest("演示分类", 1, "ENABLED"));
        var product = catalogService.createProduct(new ProductWriteRequest(
                category.id(),
                "演示激活码",
                "用于验证自动交付流程的演示商品，售价 1.00 元。",
                null,
                100,
                "LICENSE",
                "ON_SALE"));
        inventoryService.importItems(new InventoryImportRequest(
                product.id(), List.of("DEMO-KEY-1001", "DEMO-KEY-1002", "DEMO-KEY-1003"), "演示导入"));
    }
}
