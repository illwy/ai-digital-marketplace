package com.aidigital.marketplace.node.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.aidigital.marketplace.node.api.dto.NodeProvisionJobView;
import com.aidigital.marketplace.node.infrastructure.entity.NodeProductPlanEntity;
import com.aidigital.marketplace.node.infrastructure.entity.NodeProvisionJobEntity;
import com.aidigital.marketplace.node.infrastructure.entity.NodeSubscriptionEntity;
import com.aidigital.marketplace.node.infrastructure.mapper.NodeProvisionJobMapper;
import com.aidigital.marketplace.node.infrastructure.mapper.NodeSubscriptionMapper;
import com.aidigital.marketplace.order.application.OrderService;
import com.aidigital.marketplace.order.infrastructure.entity.OrderEntity;
import com.aidigital.marketplace.order.infrastructure.entity.OrderItemEntity;
import com.aidigital.marketplace.order.infrastructure.mapper.OrderItemMapper;
import com.aidigital.marketplace.shared.web.ApiException;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;

@ExtendWith(MockitoExtension.class)
class NodeProvisioningServiceTest {

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, NodeProvisionJobEntity.class);
        TableInfoHelper.initTableInfo(assistant, NodeSubscriptionEntity.class);
    }

    @Mock
    private NodePlanService nodePlanService;

    @Mock
    private NodeSubscriptionMapper subscriptionMapper;

    @Mock
    private NodeProvisionJobMapper jobMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private OrderService orderService;

    @Mock
    private TwoSUiClientAdapter adapter;

    @InjectMocks
    private NodeProvisioningService nodeProvisioningService;

    @Test
    void enqueueInsertsUniqueJob() {
        OrderEntity order = order(10L, 3L);
        when(orderItemMapper.selectOne(any())).thenReturn(item(10L, 7L));
        when(nodePlanService.requireConfigured(7L)).thenReturn(plan(7L));
        when(jobMapper.selectOne(any())).thenReturn(null);

        nodeProvisioningService.enqueuePaidOrder(order);

        ArgumentCaptor<NodeProvisionJobEntity> captor = ArgumentCaptor.forClass(NodeProvisionJobEntity.class);
        verify(jobMapper).insert(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(10L);
        assertThat(captor.getValue().getAction()).isEqualTo("PROVISION");
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void enqueueIsIdempotentWhenJobExists() {
        when(orderItemMapper.selectOne(any())).thenReturn(item(10L, 7L));
        when(nodePlanService.requireConfigured(7L)).thenReturn(plan(7L));
        when(jobMapper.selectOne(any())).thenReturn(new NodeProvisionJobEntity());

        nodeProvisioningService.enqueuePaidOrder(order(10L, 3L));

        verify(jobMapper, never()).insert(any(NodeProvisionJobEntity.class));
    }

    @Test
    void enqueueSwallowsDuplicateKey() {
        when(orderItemMapper.selectOne(any())).thenReturn(item(10L, 7L));
        when(nodePlanService.requireConfigured(7L)).thenReturn(plan(7L));
        when(jobMapper.selectOne(any())).thenReturn(null);
        when(jobMapper.insert(any(NodeProvisionJobEntity.class))).thenThrow(new DuplicateKeyException("dup"));

        nodeProvisioningService.enqueuePaidOrder(order(10L, 3L));

        verify(jobMapper).insert(any(NodeProvisionJobEntity.class));
    }

    @Test
    void retryDoesNotChangeSuccessfulJob() {
        NodeProvisionJobEntity job = job(1L, "SUCCESS", 1);
        when(jobMapper.selectById(1L)).thenReturn(job);

        NodeProvisionJobView view = nodeProvisioningService.retryJob(1L);

        assertThat(view.status()).isEqualTo("SUCCESS");
        verify(jobMapper, never()).updateById(any(NodeProvisionJobEntity.class));
    }

    @Test
    void retryMarksFailedJobForRerun() {
        NodeProvisionJobEntity job = job(1L, "FAILED", 5);
        when(jobMapper.selectById(1L)).thenReturn(job);

        NodeProvisionJobView view = nodeProvisioningService.retryJob(1L);

        assertThat(view.status()).isEqualTo("RETRY");
        verify(jobMapper).updateById(job);
    }

    @Test
    void retryMissingJobFails() {
        when(jobMapper.selectById(8L)).thenReturn(null);
        assertThatThrownBy(() -> nodeProvisioningService.retryJob(8L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("NODE_JOB_NOT_FOUND");
    }

    @Test
    void getMineRejectsOtherUsers() {
        NodeSubscriptionEntity entity = new NodeSubscriptionEntity();
        entity.setId(4L);
        entity.setUserId(2L);
        when(subscriptionMapper.selectById(4L)).thenReturn(entity);

        assertThatThrownBy(() -> nodeProvisioningService.getMine(3L, 4L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("NODE_SUBSCRIPTION_NOT_FOUND");
    }

    @Test
    void processPendingJobsCreatesSubscriptionAndDelivers() {
        NodeProvisionJobEntity job = job(1L, "PENDING", 0);
        when(jobMapper.selectList(any())).thenReturn(java.util.List.of(job));
        when(jobMapper.update(isNull(), any())).thenReturn(1);
        when(nodePlanService.requireEnabled(7L)).thenReturn(plan(7L));
        when(subscriptionMapper.selectOne(any())).thenReturn(null);
        when(adapter.provision(any(), isNull(), any(), anyLong(), anyLong(), anyInt()))
                .thenReturn(new TwoSUiClientAdapter.RemoteClient(
                        99L, "market-u3-p7", "[{\"uri\":\"vless://x\"}]", "vless://x"));
        OrderEntity order = order(10L, 3L);
        when(orderService.require(10L)).thenReturn(order);

        nodeProvisioningService.processPendingJobs();

        ArgumentCaptor<NodeSubscriptionEntity> captor = ArgumentCaptor.forClass(NodeSubscriptionEntity.class);
        verify(subscriptionMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(captor.getValue().getProviderClientId()).isEqualTo(99L);
        assertThat(captor.getValue().getSubscriptionUrl()).isEqualTo("vless://x");
        assertThat(job.getStatus()).isEqualTo("SUCCESS");
        verify(orderService).markPaidAndDeliver(order, "DELIVERED");
    }

    @Test
    void processPendingJobsRenewsExistingClient() {
        NodeProvisionJobEntity job = job(1L, "PENDING", 0);
        when(jobMapper.selectList(any())).thenReturn(java.util.List.of(job));
        when(jobMapper.update(isNull(), any())).thenReturn(1);
        when(nodePlanService.requireEnabled(7L)).thenReturn(plan(7L));
        NodeSubscriptionEntity existing = new NodeSubscriptionEntity();
        existing.setId(4L);
        existing.setClientName("market-u3-p7");
        existing.setProviderClientId(99L);
        existing.setExpiresAt(LocalDateTime.now().plusDays(10));
        when(subscriptionMapper.selectOne(any())).thenReturn(existing);
        when(adapter.provision(any(), any(), any(), anyLong(), anyLong(), anyInt()))
                .thenReturn(new TwoSUiClientAdapter.RemoteClient(
                        99L, "market-u3-p7", "[]", "vless://renew"));
        when(orderService.require(10L)).thenReturn(order(10L, 3L));

        nodeProvisioningService.processPendingJobs();

        verify(subscriptionMapper).updateById(existing);
        verify(subscriptionMapper, never()).insert(any(NodeSubscriptionEntity.class));
        assertThat(existing.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(30).minusMinutes(1));
    }

    @Test
    void processPendingJobsRetriesThenFailsOrder() {
        NodeProvisionJobEntity job = job(1L, "PENDING", 4);
        when(jobMapper.selectList(any())).thenReturn(java.util.List.of(job));
        when(jobMapper.update(isNull(), any())).thenReturn(1);
        when(nodePlanService.requireEnabled(7L)).thenReturn(plan(7L));
        when(subscriptionMapper.selectOne(any())).thenReturn(null);
        when(adapter.provision(any(), nullable(NodeSubscriptionEntity.class), any(), anyLong(), anyLong(), anyInt()))
                .thenThrow(new TwoSUiClientAdapter.NodeProviderException("2S-UI down"));
        OrderEntity order = order(10L, 3L);
        when(orderService.require(10L)).thenReturn(order);

        nodeProvisioningService.processPendingJobs();

        assertThat(job.getStatus()).isEqualTo("FAILED");
        assertThat(job.getAttempts()).isEqualTo(5);
        verify(orderService).markPaidAndDeliver(order, "FAILED");
        verify(subscriptionMapper, never()).insert(any(NodeSubscriptionEntity.class));
    }

    @Test
    void expireSubscriptionsDisablesRemoteClient() {
        NodeSubscriptionEntity expired = new NodeSubscriptionEntity();
        expired.setId(4L);
        expired.setProductId(7L);
        expired.setStatus("ACTIVE");
        expired.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(subscriptionMapper.selectList(any())).thenReturn(java.util.List.of(expired));
        when(nodePlanService.requireConfigured(7L)).thenReturn(plan(7L));

        nodeProvisioningService.expireSubscriptions();

        verify(adapter).disable(any(), any());
        assertThat(expired.getStatus()).isEqualTo("EXPIRED");
        verify(subscriptionMapper).updateById(expired);
    }

    @Test
    void expireSubscriptionsKeepsActiveWhenDisableFails() {
        NodeSubscriptionEntity expired = new NodeSubscriptionEntity();
        expired.setId(4L);
        expired.setProductId(7L);
        expired.setStatus("ACTIVE");
        expired.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(subscriptionMapper.selectList(any())).thenReturn(java.util.List.of(expired));
        when(nodePlanService.requireConfigured(7L)).thenReturn(plan(7L));
        org.mockito.Mockito.doThrow(new TwoSUiClientAdapter.NodeProviderException("down"))
                .when(adapter)
                .disable(any(), any());

        nodeProvisioningService.expireSubscriptions();

        assertThat(expired.getStatus()).isEqualTo("ACTIVE");
        verify(subscriptionMapper, never()).updateById(any(NodeSubscriptionEntity.class));
    }

    private static OrderEntity order(long id, long userId) {
        OrderEntity entity = new OrderEntity();
        entity.setId(id);
        entity.setUserId(userId);
        return entity;
    }

    private static OrderItemEntity item(long orderId, long productId) {
        OrderItemEntity entity = new OrderItemEntity();
        entity.setOrderId(orderId);
        entity.setProductId(productId);
        return entity;
    }

    private static NodeProductPlanEntity plan(long productId) {
        NodeProductPlanEntity entity = new NodeProductPlanEntity();
        entity.setProductId(productId);
        entity.setDurationDays(30);
        entity.setTrafficBytes(1024L);
        entity.setDeviceLimit(3);
        entity.setEnabled(true);
        return entity;
    }

    private static NodeProvisionJobEntity job(long id, String status, int attempts) {
        NodeProvisionJobEntity entity = new NodeProvisionJobEntity();
        entity.setId(id);
        entity.setOrderId(10L);
        entity.setUserId(3L);
        entity.setProductId(7L);
        entity.setAction("PROVISION");
        entity.setStatus(status);
        entity.setAttempts(attempts);
        entity.setNextRunAt(LocalDateTime.now().minusSeconds(1));
        return entity;
    }
}
