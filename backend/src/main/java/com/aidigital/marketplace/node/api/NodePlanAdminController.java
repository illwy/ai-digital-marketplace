package com.aidigital.marketplace.node.api;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.node.api.dto.NodePlanWriteRequest;
import com.aidigital.marketplace.node.application.NodePlanService;
import com.aidigital.marketplace.node.infrastructure.mapper.NodeProductPlanMapper;
import com.aidigital.marketplace.shared.web.DataResponse;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/admin/node-plans")
public class NodePlanAdminController {

    private final NodePlanService nodePlanService;
    private final NodeProductPlanMapper planMapper;

    public NodePlanAdminController(NodePlanService nodePlanService, NodeProductPlanMapper planMapper) {
        this.nodePlanService = nodePlanService;
        this.planMapper = planMapper;
    }

    @PostMapping
    public DataResponse<?> save(@Valid @RequestBody NodePlanWriteRequest request) {
        return new DataResponse<>(nodePlanService.save(request));
    }

    @GetMapping("/{productId}")
    public DataResponse<?> get(@PathVariable Long productId) {
        return new DataResponse<>(planMapper.selectById(productId));
    }
}
