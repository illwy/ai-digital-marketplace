package com.aidigital.marketplace.order.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidigital.marketplace.catalog.application.CatalogService;
import com.aidigital.marketplace.catalog.infrastructure.entity.ProductEntity;
import com.aidigital.marketplace.inventory.application.InventoryService;
import com.aidigital.marketplace.inventory.infrastructure.entity.InventoryEntity;
import com.aidigital.marketplace.order.api.dto.OrderView;
import com.aidigital.marketplace.order.infrastructure.entity.OrderEntity;
import com.aidigital.marketplace.order.infrastructure.entity.OrderItemEntity;
import com.aidigital.marketplace.order.infrastructure.mapper.OrderItemMapper;
import com.aidigital.marketplace.order.infrastructure.mapper.OrderMapper;
import com.aidigital.marketplace.shared.application.BizNos;
import com.aidigital.marketplace.shared.web.ApiException;
import com.aidigital.marketplace.shared.web.ListResponse;
import com.aidigital.marketplace.shared.web.PageQuery;
import com.aidigital.marketplace.shared.web.Pagination;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CatalogService catalogService;
    private final InventoryService inventoryService;

    public OrderService(
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            CatalogService catalogService,
            InventoryService inventoryService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.catalogService = catalogService;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public OrderView create(Long userId, Long productId) {
        ProductEntity product = catalogService.requireOnSale(productId);
        OrderEntity pending = orderMapper.findPendingByUserAndProduct(userId, productId);
        if (pending != null) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ORDER_PENDING",
                    "该商品已有未支付订单",
                    Map.of("orderId", pending.getId()));
        }
        LocalDateTime now = LocalDateTime.now();
        OrderEntity order = new OrderEntity();
        order.setOrderNo(BizNos.next("ORD"));
        order.setUserId(userId);
        order.setAmountFen(product.getPriceFen());
        order.setPayStatus("PENDING");
        order.setDeliveryStatus("WAITING");
        order.setAftersaleStatus("NONE");
        order.setExpireAt(now.plusMinutes(15));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderMapper.insert(order);

        OrderItemEntity item = new OrderItemEntity();
        item.setOrderId(order.getId());
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setPriceFen(product.getPriceFen());
        item.setQuantity(1);
        orderItemMapper.insert(item);

        InventoryEntity locked = inventoryService.lockOne(productId, order.getId());
        order.setInventoryId(locked.getId());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return toView(order, item);
    }

    public ListResponse<OrderView> listMine(Long userId, int page, int pageSize) {
        return list(userId, null, null, null, page, pageSize);
    }

    public ListResponse<OrderView> listAdmin(
            String payStatus, String deliveryStatus, String orderNo, int page, int pageSize) {
        return list(null, payStatus, deliveryStatus, orderNo, page, pageSize);
    }

    public OrderView getMine(Long userId, Long id) {
        OrderEntity order = require(id);
        if (!order.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在");
        }
        return toView(order, requireItem(order.getId()));
    }

    public OrderView getAdmin(Long id) {
        OrderEntity order = require(id);
        return toView(order, requireItem(order.getId()));
    }

    public OrderEntity require(Long id) {
        OrderEntity order = orderMapper.selectById(id);
        if (order == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在");
        }
        return order;
    }

    @Transactional
    public OrderView cancel(Long id, Long actorUserId, boolean admin, String terminalStatus) {
        OrderEntity order = require(id);
        if (!admin && !order.getUserId().equals(actorUserId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在");
        }
        if (!"PENDING".equals(order.getPayStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "ORDER_NOT_PENDING", "只能关闭未支付订单");
        }
        order.setPayStatus(terminalStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        if (order.getInventoryId() != null) {
            inventoryService.release(order.getInventoryId(), order.getId());
        }
        return toView(order, requireItem(order.getId()));
    }

    @Transactional
    public void markPaidAndDeliver(OrderEntity order, String deliveryStatus) {
        order.setPayStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        order.setDeliveryStatus(deliveryStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Transactional
    public void markAftersaleOpen(Long orderId) {
        OrderEntity order = require(orderId);
        order.setAftersaleStatus("OPEN");
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Transactional
    public void markAftersaleClosed(Long orderId) {
        OrderEntity order = require(orderId);
        order.setAftersaleStatus("CLOSED");
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void expireDueOrders() {
        List<OrderEntity> due = orderMapper.selectList(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getPayStatus, "PENDING")
                .lt(OrderEntity::getExpireAt, LocalDateTime.now()));
        for (OrderEntity order : due) {
            if (!"PENDING".equals(order.getPayStatus())) {
                continue;
            }
            order.setPayStatus("EXPIRED");
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            if (order.getInventoryId() != null) {
                inventoryService.release(order.getInventoryId(), order.getId());
            }
        }
    }

    private ListResponse<OrderView> list(
            Long userId, String payStatus, String deliveryStatus, String orderNo, int page, int pageSize) {
        Page<OrderEntity> mp = PageQuery.of(page, pageSize);
        LambdaQueryWrapper<OrderEntity> query = new LambdaQueryWrapper<OrderEntity>()
                .eq(userId != null, OrderEntity::getUserId, userId)
                .eq(payStatus != null && !payStatus.isBlank(), OrderEntity::getPayStatus, payStatus)
                .eq(deliveryStatus != null && !deliveryStatus.isBlank(), OrderEntity::getDeliveryStatus, deliveryStatus)
                .eq(orderNo != null && !orderNo.isBlank(), OrderEntity::getOrderNo, orderNo.trim())
                .orderByDesc(OrderEntity::getId);
        Page<OrderEntity> result = orderMapper.selectPage(mp, query);
        List<OrderView> items = result.getRecords().stream()
                .map(order -> toView(order, requireItem(order.getId())))
                .toList();
        return new ListResponse<>(
                items, Pagination.of((int) result.getCurrent(), (int) result.getSize(), result.getTotal()));
    }

    private OrderItemEntity requireItem(Long orderId) {
        OrderItemEntity item = orderItemMapper.selectOne(
                new LambdaQueryWrapper<OrderItemEntity>().eq(OrderItemEntity::getOrderId, orderId));
        if (item == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_ITEM_MISSING", "订单快照缺失");
        }
        return item;
    }

    private OrderView toView(OrderEntity order, OrderItemEntity item) {
        return new OrderView(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                order.getAmountFen(),
                order.getPayStatus(),
                order.getDeliveryStatus(),
                order.getAftersaleStatus(),
                order.getInventoryId(),
                item.getProductId(),
                item.getProductName(),
                order.getExpireAt(),
                order.getPaidAt(),
                order.getCreatedAt());
    }
}
