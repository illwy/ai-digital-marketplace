package com.aidigital.marketplace.delivery.application;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidigital.marketplace.delivery.api.dto.DeliveryView;
import com.aidigital.marketplace.delivery.infrastructure.entity.DeliveryEntity;
import com.aidigital.marketplace.delivery.infrastructure.mapper.DeliveryMapper;
import com.aidigital.marketplace.inventory.infrastructure.entity.InventoryEntity;
import com.aidigital.marketplace.inventory.infrastructure.mapper.InventoryMapper;
import com.aidigital.marketplace.order.infrastructure.entity.OrderEntity;
import com.aidigital.marketplace.order.infrastructure.entity.OrderItemEntity;
import com.aidigital.marketplace.order.infrastructure.mapper.OrderItemMapper;
import com.aidigital.marketplace.order.infrastructure.mapper.OrderMapper;
import com.aidigital.marketplace.shared.web.ApiException;
import com.aidigital.marketplace.shared.web.ContentMasker;
import com.aidigital.marketplace.shared.web.ListResponse;
import com.aidigital.marketplace.shared.web.PageQuery;
import com.aidigital.marketplace.shared.web.Pagination;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Service
public class DeliveryService {

    private final DeliveryMapper deliveryMapper;
    private final InventoryMapper inventoryMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    public DeliveryService(
            DeliveryMapper deliveryMapper,
            InventoryMapper inventoryMapper,
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper) {
        this.deliveryMapper = deliveryMapper;
        this.inventoryMapper = inventoryMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Transactional
    public DeliveryEntity create(Long orderId, Long userId, Long inventoryId, String status, String remark) {
        DeliveryEntity existing = deliveryMapper.selectOne(
                new LambdaQueryWrapper<DeliveryEntity>().eq(DeliveryEntity::getOrderId, orderId));
        if (existing != null) {
            return existing;
        }
        DeliveryEntity record = new DeliveryEntity();
        record.setOrderId(orderId);
        record.setUserId(userId);
        record.setInventoryId(inventoryId);
        record.setStatus(status);
        record.setRemark(remark);
        record.setDeliveredAt(LocalDateTime.now());
        deliveryMapper.insert(record);
        return record;
    }

    public ListResponse<DeliveryView> listMine(Long userId, Long orderId, int page, int pageSize) {
        return list(userId, orderId, false, page, pageSize);
    }

    public ListResponse<DeliveryView> listAdmin(Long orderId, int page, int pageSize) {
        return list(null, orderId, false, page, pageSize);
    }

    public DeliveryView getMine(Long userId, Long id) {
        DeliveryEntity record = require(id);
        if (!record.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "DELIVERY_NOT_FOUND", "交付记录不存在");
        }
        return toView(record, false);
    }

    @Transactional
    public DeliveryView remarkFailed(Long id, String remark) {
        DeliveryEntity record = require(id);
        record.setRemark(remark);
        deliveryMapper.updateById(record);
        return toView(record, true);
    }

    private ListResponse<DeliveryView> list(Long userId, Long orderId, boolean maskContent, int page, int pageSize) {
        Page<DeliveryEntity> mp = PageQuery.of(page, pageSize);
        LambdaQueryWrapper<DeliveryEntity> query = new LambdaQueryWrapper<DeliveryEntity>()
                .eq(userId != null, DeliveryEntity::getUserId, userId)
                .eq(orderId != null, DeliveryEntity::getOrderId, orderId)
                .orderByDesc(DeliveryEntity::getId);
        Page<DeliveryEntity> result = deliveryMapper.selectPage(mp, query);
        List<DeliveryView> items =
                result.getRecords().stream().map(record -> toView(record, maskContent)).toList();
        return new ListResponse<>(
                items, Pagination.of((int) result.getCurrent(), (int) result.getSize(), result.getTotal()));
    }

    private DeliveryView toView(DeliveryEntity record, boolean mask) {
        InventoryEntity inventory = inventoryMapper.selectById(record.getInventoryId());
        String content = inventory == null ? "" : inventory.getContent();
        if (mask) {
            content = ContentMasker.mask(content);
        }
        OrderEntity order = orderMapper.selectById(record.getOrderId());
        OrderItemEntity item = orderItemMapper.selectOne(
                new LambdaQueryWrapper<OrderItemEntity>().eq(OrderItemEntity::getOrderId, record.getOrderId()));
        return new DeliveryView(
                record.getId(),
                record.getOrderId(),
                record.getInventoryId(),
                order == null ? "" : order.getOrderNo(),
                item == null ? "" : item.getProductName(),
                record.getStatus(),
                content,
                record.getRemark(),
                record.getDeliveredAt());
    }

    private DeliveryEntity require(Long id) {
        DeliveryEntity record = deliveryMapper.selectById(id);
        if (record == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "DELIVERY_NOT_FOUND", "交付记录不存在");
        }
        return record;
    }
}
