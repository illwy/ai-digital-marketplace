package com.aidigital.marketplace.node.application;

import java.net.URI;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidigital.marketplace.catalog.infrastructure.entity.ProductEntity;
import com.aidigital.marketplace.catalog.infrastructure.mapper.ProductMapper;
import com.aidigital.marketplace.node.api.dto.NodePlanWriteRequest;
import com.aidigital.marketplace.node.infrastructure.entity.NodeProductPlanEntity;
import com.aidigital.marketplace.node.infrastructure.mapper.NodeProductPlanMapper;
import com.aidigital.marketplace.shared.web.ApiException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Service
public class NodePlanService {

    private final NodeProductPlanMapper planMapper;
    private final ProductMapper productMapper;

    public NodePlanService(NodeProductPlanMapper planMapper, ProductMapper productMapper) {
        this.planMapper = planMapper;
        this.productMapper = productMapper;
    }

    @Transactional
    public NodeProductPlanEntity save(NodePlanWriteRequest request) {
        ProductEntity product = productMapper.selectById(request.productId());
        if (product == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "商品不存在");
        }
        if (!"NODE_SUBSCRIPTION".equals(product.getDeliveryType())) {
            throw new ApiException(HttpStatus.CONFLICT, "PRODUCT_NOT_NODE", "商品不是节点订阅类型");
        }
        String baseUrl = normalizeBaseUrl(request.apiBaseUrl());
        NodeProductPlanEntity entity = planMapper.selectById(request.productId());
        boolean created = entity == null;
        if (entity == null) {
            entity = new NodeProductPlanEntity();
            entity.setProductId(request.productId());
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setApiBaseUrl(baseUrl);
        entity.setWebPath(normalizeWebPath(request.webPath()));
        entity.setInboundIdsJson(request.inboundIdsJson().trim());
        entity.setTrafficBytes(request.trafficBytes());
        entity.setDurationDays(request.durationDays());
        entity.setDeviceLimit(request.deviceLimit());
        entity.setEnabled(request.enabled());
        entity.setUpdatedAt(LocalDateTime.now());
        if (created) {
            planMapper.insert(entity);
        } else {
            planMapper.updateById(entity);
        }
        return entity;
    }

    public NodeProductPlanEntity requireEnabled(Long productId) {
        NodeProductPlanEntity entity = planMapper.selectOne(new LambdaQueryWrapper<NodeProductPlanEntity>()
                .eq(NodeProductPlanEntity::getProductId, productId)
                .eq(NodeProductPlanEntity::getEnabled, true));
        if (entity == null) {
            throw new ApiException(HttpStatus.CONFLICT, "NODE_PLAN_UNAVAILABLE", "节点套餐尚未配置或已下架");
        }
        return entity;
    }

    public NodeProductPlanEntity requireConfigured(Long productId) {
        NodeProductPlanEntity entity = planMapper.selectById(productId);
        if (entity == null) {
            throw new ApiException(HttpStatus.CONFLICT, "NODE_PLAN_UNAVAILABLE", "节点套餐尚未配置");
        }
        return entity;
    }

    private String normalizeBaseUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            if (uri.getScheme() == null || uri.getHost() == null
                    || (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException();
            }
            return URI.create(uri.getScheme() + "://" + uri.getRawAuthority()).toString();
        } catch (RuntimeException ex) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "NODE_BASE_URL_INVALID", "2S-UI 地址无效");
        }
    }

    private String normalizeWebPath(String value) {
        String path = value == null || value.isBlank() ? "/app/" : value.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.endsWith("/")) {
            path += "/";
        }
        return path;
    }
}
