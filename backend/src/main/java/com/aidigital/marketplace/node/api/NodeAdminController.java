package com.aidigital.marketplace.node.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.node.api.dto.NodeProvisionJobView;
import com.aidigital.marketplace.node.api.dto.NodeSubscriptionAdminView;
import com.aidigital.marketplace.node.application.NodeProvisioningService;
import com.aidigital.marketplace.shared.web.DataResponse;

@RestController
@RequestMapping("/api/v1/admin/node")
public class NodeAdminController {

    private final NodeProvisioningService nodeProvisioningService;

    public NodeAdminController(NodeProvisioningService nodeProvisioningService) {
        this.nodeProvisioningService = nodeProvisioningService;
    }

    @GetMapping("/jobs")
    public DataResponse<List<NodeProvisionJobView>> jobs(@RequestParam(required = false) String status) {
        return new DataResponse<>(nodeProvisioningService.listJobs(status));
    }

    @GetMapping("/subscriptions")
    public DataResponse<List<NodeSubscriptionAdminView>> subscriptions(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status) {
        return new DataResponse<>(nodeProvisioningService.listAll(username, status));
    }

    @PostMapping("/jobs/{id}/retry")
    public DataResponse<NodeProvisionJobView> retry(@PathVariable Long id) {
        return new DataResponse<>(nodeProvisioningService.retryJob(id));
    }
}
