package com.aidigital.marketplace.node.application;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidigital.marketplace.node.api.dto.NodeSubscriptionView;
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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.dao.DuplicateKeyException;

/**
 * Delivers node products asynchronously. Payment callbacks only enqueue a
 * unique job, so a slow or temporarily unavailable 2S-UI never blocks the
 * payment request and cannot cause a second client to be created.
 */
@Service
public class NodeProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(NodeProvisioningService.class);
    private static final String ACTION_PROVISION = "PROVISION";
    private static final int MAX_ATTEMPTS = 5;

    private final NodePlanService nodePlanService;
    private final NodeSubscriptionMapper subscriptionMapper;
    private final NodeProvisionJobMapper jobMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderService orderService;
    private final TwoSUiClientAdapter adapter;

    public NodeProvisioningService(
            NodePlanService nodePlanService,
            NodeSubscriptionMapper subscriptionMapper,
            NodeProvisionJobMapper jobMapper,
            OrderItemMapper orderItemMapper,
            OrderService orderService,
            TwoSUiClientAdapter adapter) {
        this.nodePlanService = nodePlanService;
        this.subscriptionMapper = subscriptionMapper;
        this.jobMapper = jobMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderService = orderService;
        this.adapter = adapter;
    }

    @Transactional
    public void enqueuePaidOrder(OrderEntity order) {
        OrderItemEntity item = requireItem(order.getId());
        NodeProductPlanEntity plan = nodePlanService.requireConfigured(item.getProductId());
        NodeProvisionJobEntity existing = jobMapper.selectOne(new LambdaQueryWrapper<NodeProvisionJobEntity>()
                .eq(NodeProvisionJobEntity::getOrderId, order.getId())
                .eq(NodeProvisionJobEntity::getAction, ACTION_PROVISION));
        if (existing != null) {
            return;
        }
        NodeProvisionJobEntity job = new NodeProvisionJobEntity();
        job.setOrderId(order.getId());
        job.setUserId(order.getUserId());
        job.setProductId(plan.getProductId());
        job.setAction(ACTION_PROVISION);
        job.setStatus("PENDING");
        job.setAttempts(0);
        job.setNextRunAt(LocalDateTime.now());
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        try {
            jobMapper.insert(job);
        } catch (DuplicateKeyException ignored) {
            // A repeated payment notification raced with the first callback.
        }
    }

    public List<NodeSubscriptionView> listMine(Long userId) {
        return subscriptionMapper.selectList(new LambdaQueryWrapper<NodeSubscriptionEntity>()
                        .eq(NodeSubscriptionEntity::getUserId, userId)
                        .orderByDesc(NodeSubscriptionEntity::getId))
                .stream().map(NodeSubscriptionView::from).toList();
    }

    public NodeSubscriptionView getMine(Long userId, Long id) {
        NodeSubscriptionEntity entity = subscriptionMapper.selectById(id);
        if (entity == null || !entity.getUserId().equals(userId)) {
            throw new ApiException(org.springframework.http.HttpStatus.NOT_FOUND,
                    "NODE_SUBSCRIPTION_NOT_FOUND", "节点订阅不存在");
        }
        return NodeSubscriptionView.from(entity);
    }

    public List<NodeProvisionJobView> listJobs(String status) {
        return jobMapper.selectList(new LambdaQueryWrapper<NodeProvisionJobEntity>()
                        .eq(status != null && !status.isBlank(), NodeProvisionJobEntity::getStatus, status)
                        .orderByDesc(NodeProvisionJobEntity::getId)
                        .last("LIMIT 100"))
                .stream().map(NodeProvisionJobView::from).toList();
    }

    @Transactional
    public NodeProvisionJobView retryJob(Long id) {
        NodeProvisionJobEntity job = jobMapper.selectById(id);
        if (job == null) {
            throw new ApiException(org.springframework.http.HttpStatus.NOT_FOUND,
                    "NODE_JOB_NOT_FOUND", "开通任务不存在");
        }
        if ("SUCCESS".equals(job.getStatus())) {
            return NodeProvisionJobView.from(job);
        }
        job.setStatus("RETRY");
        job.setNextRunAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        jobMapper.updateById(job);
        return NodeProvisionJobView.from(job);
    }

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void processPendingJobs() {
        List<NodeProvisionJobEntity> jobs = jobMapper.selectList(new LambdaQueryWrapper<NodeProvisionJobEntity>()
                .in(NodeProvisionJobEntity::getStatus, List.of("PENDING", "RETRY"))
                .le(NodeProvisionJobEntity::getNextRunAt, LocalDateTime.now())
                .orderByAsc(NodeProvisionJobEntity::getId)
                .last("LIMIT 10"));
        for (NodeProvisionJobEntity job : jobs) {
            if (!claim(job)) {
                continue;
            }
            try {
                provision(job);
            } catch (RuntimeException ex) {
                fail(job, ex);
            }
        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expireSubscriptions() {
        List<NodeSubscriptionEntity> expired = subscriptionMapper.selectList(new LambdaQueryWrapper<NodeSubscriptionEntity>()
                .eq(NodeSubscriptionEntity::getStatus, "ACTIVE")
                .lt(NodeSubscriptionEntity::getExpiresAt, LocalDateTime.now())
                .last("LIMIT 20"));
        for (NodeSubscriptionEntity subscription : expired) {
            try {
                NodeProductPlanEntity plan = nodePlanService.requireConfigured(subscription.getProductId());
                adapter.disable(plan, subscription);
                subscription.setStatus("EXPIRED");
                subscription.setUpdatedAt(LocalDateTime.now());
                subscriptionMapper.updateById(subscription);
            } catch (RuntimeException ex) {
                log.warn("Expire node subscription {} failed", subscription.getId());
            }
        }
    }

    private boolean claim(NodeProvisionJobEntity job) {
        int changed = jobMapper.update(null, new LambdaUpdateWrapper<NodeProvisionJobEntity>()
                .eq(NodeProvisionJobEntity::getId, job.getId())
                .in(NodeProvisionJobEntity::getStatus, List.of("PENDING", "RETRY"))
                .set(NodeProvisionJobEntity::getStatus, "RUNNING")
                .set(NodeProvisionJobEntity::getUpdatedAt, LocalDateTime.now()));
        return changed == 1;
    }

    @Transactional
    protected void provision(NodeProvisionJobEntity job) {
        NodeProductPlanEntity plan = nodePlanService.requireEnabled(job.getProductId());
        NodeSubscriptionEntity existing = subscriptionMapper.selectOne(new LambdaQueryWrapper<NodeSubscriptionEntity>()
                .eq(NodeSubscriptionEntity::getUserId, job.getUserId())
                .eq(NodeSubscriptionEntity::getProductId, job.getProductId()));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = existing == null || existing.getExpiresAt() == null
                || existing.getExpiresAt().isBefore(now) ? now : existing.getExpiresAt();
        LocalDateTime expiresAt = base.plusDays(plan.getDurationDays());
        String clientName = existing == null || existing.getClientName() == null
                ? "market-u" + job.getUserId() + "-p" + job.getProductId()
                : existing.getClientName();
        TwoSUiClientAdapter.RemoteClient remote = adapter.provision(
                plan,
                existing,
                clientName,
                plan.getTrafficBytes(),
                expiresAt.atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
                plan.getDeviceLimit());

        NodeSubscriptionEntity target = existing == null ? new NodeSubscriptionEntity() : existing;
        target.setUserId(job.getUserId());
        target.setProductId(job.getProductId());
        target.setLastOrderId(job.getOrderId());
        target.setClientName(clientName);
        target.setProviderClientId(remote.id());
        target.setTrafficBytes(plan.getTrafficBytes());
        target.setDeviceLimit(plan.getDeviceLimit());
        target.setExpiresAt(expiresAt);
        target.setSubscriptionUrl(remote.primaryUri());
        target.setClashConfig(remote.linksJson());
        target.setStatus("ACTIVE");
        target.setLastError(null);
        target.setUpdatedAt(now);
        if (existing == null) {
            target.setCreatedAt(now);
            subscriptionMapper.insert(target);
        } else {
            subscriptionMapper.updateById(target);
        }
        job.setStatus("SUCCESS");
        job.setLastError(null);
        job.setUpdatedAt(LocalDateTime.now());
        jobMapper.updateById(job);
        OrderEntity order = orderService.require(job.getOrderId());
        orderService.markPaidAndDeliver(order, "DELIVERED");
    }

    private void fail(NodeProvisionJobEntity job, RuntimeException ex) {
        int attempts = job.getAttempts() == null ? 0 : job.getAttempts();
        attempts++;
        job.setAttempts(attempts);
        job.setStatus(attempts >= MAX_ATTEMPTS ? "FAILED" : "RETRY");
        job.setNextRunAt(LocalDateTime.now().plusSeconds(Math.min(300, attempts * 30L)));
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        job.setLastError(message.substring(0, Math.min(message.length(), 500)));
        job.setUpdatedAt(LocalDateTime.now());
        jobMapper.updateById(job);
        if (attempts >= MAX_ATTEMPTS) {
            OrderEntity order = orderService.require(job.getOrderId());
            orderService.markPaidAndDeliver(order, "FAILED");
        }
        log.warn("Node provisioning job {} failed (attempt {})", job.getId(), attempts);
    }

    private OrderItemEntity requireItem(Long orderId) {
        OrderItemEntity item = orderItemMapper.selectOne(new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, orderId));
        if (item == null) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "ORDER_ITEM_MISSING", "订单快照缺失");
        }
        return item;
    }
}
