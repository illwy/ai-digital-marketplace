package com.aidigital.marketplace.catalog.application;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidigital.marketplace.catalog.api.dto.CategoryView;
import com.aidigital.marketplace.catalog.api.dto.CategoryWriteRequest;
import com.aidigital.marketplace.catalog.api.dto.ProductView;
import com.aidigital.marketplace.catalog.api.dto.ProductWriteRequest;
import com.aidigital.marketplace.catalog.infrastructure.entity.CategoryEntity;
import com.aidigital.marketplace.catalog.infrastructure.entity.ProductEntity;
import com.aidigital.marketplace.catalog.infrastructure.mapper.CategoryMapper;
import com.aidigital.marketplace.catalog.infrastructure.mapper.ProductMapper;
import com.aidigital.marketplace.inventory.application.InventoryService;
import com.aidigital.marketplace.shared.web.ApiException;
import com.aidigital.marketplace.shared.web.ListResponse;
import com.aidigital.marketplace.shared.web.PageQuery;
import com.aidigital.marketplace.shared.web.Pagination;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Service
public class CatalogService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final InventoryService inventoryService;

    public CatalogService(
            CategoryMapper categoryMapper, ProductMapper productMapper, InventoryService inventoryService) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.inventoryService = inventoryService;
    }

    public List<CategoryView> listEnabledCategories() {
        return categoryMapper
                .selectList(new LambdaQueryWrapper<CategoryEntity>()
                        .eq(CategoryEntity::getStatus, "ENABLED")
                        .orderByAsc(CategoryEntity::getSortOrder)
                        .orderByAsc(CategoryEntity::getId))
                .stream()
                .map(CategoryView::from)
                .toList();
    }

    public List<CategoryView> listAllCategories() {
        return categoryMapper
                .selectList(new LambdaQueryWrapper<CategoryEntity>()
                        .orderByAsc(CategoryEntity::getSortOrder)
                        .orderByAsc(CategoryEntity::getId))
                .stream()
                .map(CategoryView::from)
                .toList();
    }

    @Transactional
    public CategoryView createCategory(CategoryWriteRequest request) {
        CategoryEntity entity = new CategoryEntity();
        applyCategory(entity, request);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        categoryMapper.insert(entity);
        return CategoryView.from(entity);
    }

    @Transactional
    public CategoryView updateCategory(Long id, CategoryWriteRequest request) {
        CategoryEntity entity = requireCategory(id);
        applyCategory(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(entity);
        return CategoryView.from(entity);
    }

    @Transactional
    public void deleteCategory(Long id) {
        requireCategory(id);
        Long used = productMapper.selectCount(
                new LambdaQueryWrapper<ProductEntity>().eq(ProductEntity::getCategoryId, id));
        if (used > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "CATEGORY_IN_USE", "分类下仍有商品，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    public ListResponse<ProductView> listOnSale(Long categoryId, int page, int pageSize) {
        return listProducts(categoryId, "ON_SALE", page, pageSize);
    }

    public ListResponse<ProductView> listAdmin(Long categoryId, String status, int page, int pageSize) {
        return listProducts(categoryId, status, page, pageSize);
    }

    public ProductView getOnSale(Long id) {
        ProductEntity entity = requireProduct(id);
        if (!"ON_SALE".equals(entity.getStatus())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "商品不存在");
        }
        return toView(entity);
    }

    public ProductView getAdmin(Long id) {
        return toView(requireProduct(id));
    }

    public ProductEntity requireOnSale(Long id) {
        ProductEntity entity = requireProduct(id);
        if (!"ON_SALE".equals(entity.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "PRODUCT_NOT_ON_SALE", "商品未上架");
        }
        return entity;
    }

    @Transactional
    public ProductView createProduct(ProductWriteRequest request) {
        requireCategory(request.categoryId());
        ProductEntity entity = new ProductEntity();
        applyProduct(entity, request);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        productMapper.insert(entity);
        return toView(entity);
    }

    @Transactional
    public ProductView updateProduct(Long id, ProductWriteRequest request) {
        requireCategory(request.categoryId());
        ProductEntity entity = requireProduct(id);
        applyProduct(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(entity);
        return toView(entity);
    }

    private ListResponse<ProductView> listProducts(Long categoryId, String status, int page, int pageSize) {
        Page<ProductEntity> mp = PageQuery.of(page, pageSize);
        LambdaQueryWrapper<ProductEntity> query = new LambdaQueryWrapper<ProductEntity>()
                .eq(categoryId != null, ProductEntity::getCategoryId, categoryId)
                .eq(status != null && !status.isBlank(), ProductEntity::getStatus, status)
                .orderByDesc(ProductEntity::getId);
        Page<ProductEntity> result = productMapper.selectPage(mp, query);
        List<ProductView> items = result.getRecords().stream().map(this::toView).toList();
        return new ListResponse<>(
                items, Pagination.of((int) result.getCurrent(), (int) result.getSize(), result.getTotal()));
    }

    private ProductView toView(ProductEntity entity) {
        return ProductView.from(entity, inventoryService.countAvailable(entity.getId()));
    }

    private CategoryEntity requireCategory(Long id) {
        CategoryEntity entity = categoryMapper.selectById(id);
        if (entity == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "分类不存在");
        }
        return entity;
    }

    private ProductEntity requireProduct(Long id) {
        ProductEntity entity = productMapper.selectById(id);
        if (entity == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "商品不存在");
        }
        return entity;
    }

    private void applyCategory(CategoryEntity entity, CategoryWriteRequest request) {
        entity.setName(request.name());
        entity.setSortOrder(request.sortOrder());
        entity.setStatus(request.status());
    }

    private void applyProduct(ProductEntity entity, ProductWriteRequest request) {
        entity.setCategoryId(request.categoryId());
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setCoverUrl(request.coverUrl());
        entity.setPriceFen(request.priceFen());
        entity.setDeliveryType(request.deliveryType());
        entity.setStatus(request.status());
    }
}
