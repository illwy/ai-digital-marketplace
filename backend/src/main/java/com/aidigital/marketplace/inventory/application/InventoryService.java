package com.aidigital.marketplace.inventory.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidigital.marketplace.inventory.api.dto.InventoryImportRequest;
import com.aidigital.marketplace.inventory.api.dto.InventoryStatsView;
import com.aidigital.marketplace.inventory.api.dto.InventoryView;
import com.aidigital.marketplace.inventory.infrastructure.entity.InventoryEntity;
import com.aidigital.marketplace.inventory.infrastructure.mapper.InventoryMapper;
import com.aidigital.marketplace.shared.web.ApiException;
import com.aidigital.marketplace.shared.web.ListResponse;
import com.aidigital.marketplace.shared.web.PageQuery;
import com.aidigital.marketplace.shared.web.Pagination;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Service
public class InventoryService {

    private final InventoryMapper inventoryMapper;

    public InventoryService(InventoryMapper inventoryMapper) {
        this.inventoryMapper = inventoryMapper;
    }

    public long countAvailable(Long productId) {
        return inventoryMapper.selectCount(new LambdaQueryWrapper<InventoryEntity>()
                .eq(InventoryEntity::getProductId, productId)
                .eq(InventoryEntity::getStatus, "AVAILABLE"));
    }

    public InventoryStatsView stats(Long productId) {
        return new InventoryStatsView(
                countBy(productId, "AVAILABLE"),
                countBy(productId, "LOCKED"),
                countBy(productId, "SOLD"),
                countBy(productId, "INVALID"));
    }

    @Transactional
    public List<InventoryView> importItems(InventoryImportRequest request) {
        List<InventoryView> created = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (String raw : request.contents()) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            InventoryEntity item = new InventoryEntity();
            item.setProductId(request.productId());
            item.setContent(raw.trim());
            item.setStatus("AVAILABLE");
            item.setRemark(request.remark());
            item.setCreatedAt(now);
            item.setUpdatedAt(now);
            inventoryMapper.insert(item);
            created.add(InventoryView.from(item));
        }
        if (created.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVENTORY_EMPTY_IMPORT", "没有可导入的库存内容");
        }
        return created;
    }

    public ListResponse<InventoryView> list(Long productId, String status, Long orderId, int page, int pageSize) {
        Page<InventoryEntity> mp = PageQuery.of(page, pageSize);
        LambdaQueryWrapper<InventoryEntity> query = new LambdaQueryWrapper<InventoryEntity>()
                .eq(productId != null, InventoryEntity::getProductId, productId)
                .eq(status != null && !status.isBlank(), InventoryEntity::getStatus, status)
                .eq(orderId != null, InventoryEntity::getOrderId, orderId)
                .orderByDesc(InventoryEntity::getId);
        Page<InventoryEntity> result = inventoryMapper.selectPage(mp, query);
        List<InventoryView> items = result.getRecords().stream().map(InventoryView::from).toList();
        return new ListResponse<>(
                items, Pagination.of((int) result.getCurrent(), (int) result.getSize(), result.getTotal()));
    }

    @Transactional
    public InventoryView invalidate(Long id) {
        InventoryEntity item = inventoryMapper.selectById(id);
        if (item == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "INVENTORY_NOT_FOUND", "库存项不存在");
        }
        if (!"AVAILABLE".equals(item.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "INVENTORY_NOT_AVAILABLE", "只能作废可用库存");
        }
        item.setStatus("INVALID");
        item.setUpdatedAt(LocalDateTime.now());
        inventoryMapper.updateById(item);
        return InventoryView.from(item);
    }

    @Transactional
    public InventoryEntity lockOne(Long productId, Long orderId) {
        InventoryEntity item = inventoryMapper.selectOneAvailableForUpdate(productId);
        if (item == null) {
            throw new ApiException(HttpStatus.CONFLICT, "INVENTORY_EMPTY", "暂无库存");
        }
        item.setStatus("LOCKED");
        item.setOrderId(orderId);
        item.setLockedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        inventoryMapper.updateById(item);
        return item;
    }

    @Transactional
    public void release(Long inventoryId, Long orderId) {
        inventoryMapper.releaseLock(inventoryId, orderId);
    }

    @Transactional
    public boolean markSold(Long inventoryId, Long orderId) {
        return inventoryMapper.markSold(inventoryId, orderId) == 1;
    }

    public InventoryEntity require(Long id) {
        InventoryEntity item = inventoryMapper.selectById(id);
        if (item == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "INVENTORY_NOT_FOUND", "库存项不存在");
        }
        return item;
    }

    private long countBy(Long productId, String status) {
        return inventoryMapper.selectCount(new LambdaQueryWrapper<InventoryEntity>()
                .eq(InventoryEntity::getProductId, productId)
                .eq(InventoryEntity::getStatus, status));
    }
}
