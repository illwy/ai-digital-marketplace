package com.aidigital.marketplace.node.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.aidigital.marketplace.catalog.infrastructure.entity.ProductEntity;
import com.aidigital.marketplace.catalog.infrastructure.mapper.ProductMapper;
import com.aidigital.marketplace.node.api.dto.NodePlanWriteRequest;
import com.aidigital.marketplace.node.infrastructure.entity.NodeProductPlanEntity;
import com.aidigital.marketplace.node.infrastructure.mapper.NodeProductPlanMapper;
import com.aidigital.marketplace.shared.web.ApiException;

@ExtendWith(MockitoExtension.class)
class NodePlanServiceTest {

    @Mock
    private NodeProductPlanMapper planMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private NodePlanService nodePlanService;

    @Test
    void saveRejectsMissingProduct() {
        when(productMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> nodePlanService.save(request(9L, "https://panel.example.com")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void saveRejectsNonNodeProduct() {
        ProductEntity product = product(9L, "LICENSE");
        when(productMapper.selectById(9L)).thenReturn(product);

        assertThatThrownBy(() -> nodePlanService.save(request(9L, "https://panel.example.com")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PRODUCT_NOT_NODE");
    }

    @Test
    void saveRejectsInvalidBaseUrl() {
        when(productMapper.selectById(9L)).thenReturn(product(9L, "NODE_SUBSCRIPTION"));

        assertThatThrownBy(() -> nodePlanService.save(request(9L, "panel.example.com")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("NODE_BASE_URL_INVALID");
    }

    @Test
    void saveInsertsNormalizedPlan() {
        when(productMapper.selectById(9L)).thenReturn(product(9L, "NODE_SUBSCRIPTION"));
        when(planMapper.selectById(9L)).thenReturn(null);

        NodeProductPlanEntity saved = nodePlanService.save(
                new NodePlanWriteRequest(9L, "https://panel.example.com/app/", "panel", "[1,2]", 100L, 30, 3, true));

        assertThat(saved.getApiBaseUrl()).isEqualTo("https://panel.example.com");
        assertThat(saved.getWebPath()).isEqualTo("/panel/");
        assertThat(saved.getEnabled()).isTrue();
        verify(planMapper).insert(saved);
    }

    @Test
    void saveUpdatesExistingPlan() {
        when(productMapper.selectById(9L)).thenReturn(product(9L, "NODE_SUBSCRIPTION"));
        NodeProductPlanEntity existing = new NodeProductPlanEntity();
        existing.setProductId(9L);
        when(planMapper.selectById(9L)).thenReturn(existing);

        NodeProductPlanEntity saved = nodePlanService.save(request(9L, "http://127.0.0.1:8443"));

        assertThat(saved.getApiBaseUrl()).isEqualTo("http://127.0.0.1:8443");
        verify(planMapper).updateById(existing);
    }

    @Test
    void requireEnabledFailsWhenMissing() {
        when(planMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        assertThatThrownBy(() -> nodePlanService.requireEnabled(9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    private static ProductEntity product(long id, String deliveryType) {
        ProductEntity entity = new ProductEntity();
        entity.setId(id);
        entity.setDeliveryType(deliveryType);
        return entity;
    }

    private static NodePlanWriteRequest request(long productId, String apiBaseUrl) {
        return new NodePlanWriteRequest(productId, apiBaseUrl, "/app/", "[1]", 1L, 30, 1, true);
    }
}
