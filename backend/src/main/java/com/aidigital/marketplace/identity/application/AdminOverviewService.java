package com.aidigital.marketplace.identity.application;

import org.springframework.stereotype.Service;

import com.aidigital.marketplace.aftersale.infrastructure.entity.AfterSaleEntity;
import com.aidigital.marketplace.aftersale.infrastructure.mapper.AfterSaleMapper;
import com.aidigital.marketplace.catalog.infrastructure.entity.ProductEntity;
import com.aidigital.marketplace.catalog.infrastructure.mapper.ProductMapper;
import com.aidigital.marketplace.cms.infrastructure.entity.AnnouncementEntity;
import com.aidigital.marketplace.cms.infrastructure.mapper.AnnouncementMapper;
import com.aidigital.marketplace.identity.api.dto.AdminOverviewView;
import com.aidigital.marketplace.identity.infrastructure.mapper.UserMapper;
import com.aidigital.marketplace.inventory.infrastructure.entity.InventoryEntity;
import com.aidigital.marketplace.inventory.infrastructure.mapper.InventoryMapper;
import com.aidigital.marketplace.order.infrastructure.entity.OrderEntity;
import com.aidigital.marketplace.order.infrastructure.mapper.OrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Service
public class AdminOverviewService {

    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final OrderMapper orderMapper;
    private final AfterSaleMapper afterSaleMapper;
    private final AnnouncementMapper announcementMapper;

    public AdminOverviewService(
            UserMapper userMapper,
            ProductMapper productMapper,
            InventoryMapper inventoryMapper,
            OrderMapper orderMapper,
            AfterSaleMapper afterSaleMapper,
            AnnouncementMapper announcementMapper) {
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.inventoryMapper = inventoryMapper;
        this.orderMapper = orderMapper;
        this.afterSaleMapper = afterSaleMapper;
        this.announcementMapper = announcementMapper;
    }

    public AdminOverviewView load() {
        long users = userMapper.selectCount(null);
        long onSale = productMapper.selectCount(
                new LambdaQueryWrapper<ProductEntity>().eq(ProductEntity::getStatus, "ON_SALE"));
        long available = inventoryMapper.selectCount(
                new LambdaQueryWrapper<InventoryEntity>().eq(InventoryEntity::getStatus, "AVAILABLE"));
        long pending = orderMapper.selectCount(
                new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getPayStatus, "PENDING"));
        long paid = orderMapper.selectCount(
                new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getPayStatus, "PAID"));
        long openTickets = afterSaleMapper.selectCount(
                new LambdaQueryWrapper<AfterSaleEntity>().in(AfterSaleEntity::getStatus, "OPEN", "PROCESSING"));
        long announcements = announcementMapper.selectCount(
                new LambdaQueryWrapper<AnnouncementEntity>().eq(AnnouncementEntity::getStatus, "ENABLED"));
        return new AdminOverviewView(users, onSale, available, pending, paid, openTickets, announcements);
    }
}
