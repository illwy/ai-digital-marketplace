package com.aidigital.marketplace.aftersale.application;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidigital.marketplace.aftersale.api.dto.AfterSaleView;
import com.aidigital.marketplace.aftersale.infrastructure.entity.AfterSaleEntity;
import com.aidigital.marketplace.aftersale.infrastructure.mapper.AfterSaleMapper;
import com.aidigital.marketplace.order.api.dto.OrderView;
import com.aidigital.marketplace.order.application.OrderService;
import com.aidigital.marketplace.order.infrastructure.entity.OrderEntity;
import com.aidigital.marketplace.shared.web.ApiException;
import com.aidigital.marketplace.shared.web.ListResponse;
import com.aidigital.marketplace.shared.web.PageQuery;
import com.aidigital.marketplace.shared.web.Pagination;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Service
public class AfterSaleService {

    private final AfterSaleMapper afterSaleMapper;
    private final OrderService orderService;

    public AfterSaleService(AfterSaleMapper afterSaleMapper, OrderService orderService) {
        this.afterSaleMapper = afterSaleMapper;
        this.orderService = orderService;
    }

    @Transactional
    public AfterSaleView create(Long userId, Long orderId, String reason) {
        OrderEntity order = orderService.require(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在");
        }
        if (!"PAID".equals(order.getPayStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "ORDER_NOT_PAID", "未支付订单不能申请售后");
        }
        AfterSaleEntity open = afterSaleMapper.selectOne(new LambdaQueryWrapper<AfterSaleEntity>()
                .eq(AfterSaleEntity::getOrderId, orderId)
                .in(AfterSaleEntity::getStatus, List.of("OPEN", "PROCESSING")));
        if (open != null) {
            throw new ApiException(HttpStatus.CONFLICT, "AFTERSALE_OPEN", "该订单已有处理中的售后");
        }
        AfterSaleEntity ticket = new AfterSaleEntity();
        ticket.setOrderId(orderId);
        ticket.setUserId(userId);
        ticket.setReason(reason);
        ticket.setStatus("OPEN");
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        afterSaleMapper.insert(ticket);
        orderService.markAftersaleOpen(orderId);
        return toView(ticket);
    }

    public ListResponse<AfterSaleView> listMine(Long userId, int page, int pageSize) {
        return list(userId, null, page, pageSize);
    }

    public ListResponse<AfterSaleView> listAdmin(String status, int page, int pageSize) {
        return list(null, status, page, pageSize);
    }

    @Transactional
    public AfterSaleView handle(Long id, String status, String adminReply) {
        AfterSaleEntity ticket = afterSaleMapper.selectById(id);
        if (ticket == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "AFTERSALE_NOT_FOUND", "售后单不存在");
        }
        ticket.setStatus(status);
        ticket.setAdminReply(adminReply);
        ticket.setUpdatedAt(LocalDateTime.now());
        afterSaleMapper.updateById(ticket);
        if ("CLOSED".equals(status)) {
            orderService.markAftersaleClosed(ticket.getOrderId());
        }
        return toView(ticket);
    }

    private ListResponse<AfterSaleView> list(Long userId, String status, int page, int pageSize) {
        Page<AfterSaleEntity> mp = PageQuery.of(page, pageSize);
        LambdaQueryWrapper<AfterSaleEntity> query = new LambdaQueryWrapper<AfterSaleEntity>()
                .eq(userId != null, AfterSaleEntity::getUserId, userId)
                .orderByDesc(AfterSaleEntity::getId);
        if ("OPEN_ACTIVE".equals(status)) {
            query.in(AfterSaleEntity::getStatus, List.of("OPEN", "PROCESSING"));
        } else if (status != null && !status.isBlank()) {
            query.eq(AfterSaleEntity::getStatus, status);
        }
        Page<AfterSaleEntity> result = afterSaleMapper.selectPage(mp, query);
        List<AfterSaleView> items = result.getRecords().stream().map(this::toView).toList();
        return new ListResponse<>(
                items, Pagination.of((int) result.getCurrent(), (int) result.getSize(), result.getTotal()));
    }

    private AfterSaleView toView(AfterSaleEntity ticket) {
        OrderView order = orderService.getAdmin(ticket.getOrderId());
        return AfterSaleView.from(ticket, order.orderNo(), order.productName());
    }
}
