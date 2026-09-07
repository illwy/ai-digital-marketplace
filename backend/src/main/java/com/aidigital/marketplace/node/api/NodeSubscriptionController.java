package com.aidigital.marketplace.node.api;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.identity.security.AuthUser;
import com.aidigital.marketplace.node.api.dto.NodeSubscriptionView;
import com.aidigital.marketplace.node.application.NodeProvisioningService;
import com.aidigital.marketplace.shared.web.DataResponse;

@RestController
@RequestMapping("/api/v1/node-subscriptions")
public class NodeSubscriptionController {

    private final NodeProvisioningService nodeProvisioningService;

    public NodeSubscriptionController(NodeProvisioningService nodeProvisioningService) {
        this.nodeProvisioningService = nodeProvisioningService;
    }

    @GetMapping
    public DataResponse<List<NodeSubscriptionView>> list(@AuthenticationPrincipal AuthUser user) {
        return new DataResponse<>(nodeProvisioningService.listMine(user.getId()));
    }

    @GetMapping("/{id}")
    public DataResponse<NodeSubscriptionView> detail(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return new DataResponse<>(nodeProvisioningService.getMine(user.getId(), id));
    }
}
