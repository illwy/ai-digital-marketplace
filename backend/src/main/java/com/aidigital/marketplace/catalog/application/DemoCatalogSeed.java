package com.aidigital.marketplace.catalog.application;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.aidigital.marketplace.catalog.api.dto.CategoryWriteRequest;
import com.aidigital.marketplace.catalog.api.dto.ProductView;
import com.aidigital.marketplace.catalog.api.dto.ProductWriteRequest;
import com.aidigital.marketplace.catalog.infrastructure.entity.CategoryEntity;
import com.aidigital.marketplace.catalog.infrastructure.entity.ProductEntity;
import com.aidigital.marketplace.catalog.infrastructure.mapper.CategoryMapper;
import com.aidigital.marketplace.catalog.infrastructure.mapper.ProductMapper;
import com.aidigital.marketplace.cms.api.dto.AnnouncementWriteRequest;
import com.aidigital.marketplace.cms.application.AnnouncementService;
import com.aidigital.marketplace.inventory.api.dto.InventoryImportRequest;
import com.aidigital.marketplace.inventory.application.InventoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Component
@Order(20)
@ConditionalOnProperty(name = "app.demo-seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoCatalogSeed implements ApplicationRunner {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final CatalogService catalogService;
    private final InventoryService inventoryService;
    private final AnnouncementService announcementService;

    public DemoCatalogSeed(
            CategoryMapper categoryMapper,
            ProductMapper productMapper,
            CatalogService catalogService,
            InventoryService inventoryService,
            AnnouncementService announcementService) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.catalogService = catalogService;
        this.inventoryService = inventoryService;
        this.announcementService = announcementService;
    }

    @Override
    public void run(ApplicationArguments args) {
        long accounts = ensureCategory("账号月卡", 1);
        long licenses = ensureCategory("激活码", 2);
        long tokens = ensureCategory("API 额度", 3);
        ensureProduct(
                accounts,
                "ChatGPT Plus 月卡",
                "演示用账号月卡。发卡后立刻显示一条登录信息，也可在「卡密」里复制。",
                "/covers/chatgpt-plus.svg",
                19900,
                "ACCOUNT",
                List.of("gpt-plus-demo-A1", "gpt-plus-demo-A2", "gpt-plus-demo-A3"));
        ensureProduct(
                accounts,
                "Claude Pro 月卡",
                "演示用账号月卡，发卡后立刻可在本页和「卡密」查看。",
                "/covers/claude-pro.svg",
                15900,
                "ACCOUNT",
                List.of("claude-pro-demo-B1", "claude-pro-demo-B2"));
        ensureProduct(
                licenses,
                "演示激活码",
                "1 元演示卡密，用来走通发卡流程。下单后点「立即发卡」，卡密当场显示。",
                "/covers/demo-license.svg",
                100,
                "LICENSE",
                List.of("DEMO-KEY-1001", "DEMO-KEY-1002", "DEMO-KEY-1003", "DEMO-KEY-1004", "DEMO-KEY-1005"));
        ensureProduct(
                licenses,
                "Midjourney 年卡",
                "演示用卡密，库存有限，发卡后当场显示。",
                "/covers/midjourney-year.svg",
                39900,
                "LICENSE",
                List.of("mj-year-demo-C1", "mj-year-demo-C2"));
        ensureProduct(
                tokens,
                "OpenAI 额度包",
                "演示用额度文本，发卡后当场显示 Token。",
                "/covers/openai-token.svg",
                9900,
                "TOKEN",
                List.of("sk-demo-token-001", "sk-demo-token-002", "sk-demo-token-003"));
        retireDraftCategory();
        ensureAnnouncement();
    }

    private void ensureAnnouncement() {
        String title = "发卡说明";
        String body = "选卡 → 下单 → 点「立即发卡」→ 当场复制卡密。一单一件，虚拟库存，无需真实收款。";
        var existing = announcementService.listAll().stream()
                .filter(item -> "演示站说明".equals(item.title()) || "发卡说明".equals(item.title()))
                .findFirst();
        if (existing.isEmpty()) {
            announcementService.create(new AnnouncementWriteRequest(title, body, "ENABLED"));
        }
    }

    private long ensureCategory(String name, int sortOrder) {
        CategoryEntity existing = firstCategory(name);
        if (existing != null) {
            // 已存在的分类不回写，避免每次启动覆盖管理员的后台编辑
            return existing.getId();
        }
        return catalogService.createCategory(new CategoryWriteRequest(name, sortOrder, "ENABLED")).id();
    }

    private void ensureProduct(
            long categoryId,
            String name,
            String description,
            String coverUrl,
            int priceFen,
            String deliveryType,
            List<String> keys) {
        ProductEntity existing = firstProduct(name);
        if (existing != null) {
            // 已存在的商品不回写、不补货，避免每次启动覆盖管理员的后台编辑
            return;
        }
        ProductWriteRequest request =
                new ProductWriteRequest(categoryId, name, description, coverUrl, priceFen, deliveryType, "ON_SALE");
        ProductView created = catalogService.createProduct(request);
        inventoryService.importItems(new InventoryImportRequest(created.id(), keys, "演示库存"));
    }

    private CategoryEntity firstCategory(String name) {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<CategoryEntity>().eq(CategoryEntity::getName, name).last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private ProductEntity firstProduct(String name) {
        return productMapper.selectList(
                new LambdaQueryWrapper<ProductEntity>().eq(ProductEntity::getName, name).last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void retireDraftCategory() {
        CategoryEntity draft = firstCategory("演示分类");
        if (draft == null) {
            return;
        }
        Long used = productMapper.selectCount(
                new LambdaQueryWrapper<ProductEntity>().eq(ProductEntity::getCategoryId, draft.getId()));
        if (used == 0) {
            categoryMapper.deleteById(draft.getId());
            return;
        }
        draft.setStatus("DISABLED");
        draft.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(draft);
    }
}
