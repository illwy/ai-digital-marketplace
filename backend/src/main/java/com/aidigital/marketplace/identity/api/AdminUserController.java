package com.aidigital.marketplace.identity.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.identity.api.dto.AdminUserView;
import com.aidigital.marketplace.identity.api.dto.UserStatusRequest;
import com.aidigital.marketplace.identity.application.AdminUserService;
import com.aidigital.marketplace.shared.web.DataResponse;
import com.aidigital.marketplace.shared.web.ListResponse;
import com.aidigital.marketplace.wallet.api.dto.WalletCreditRequest;
import com.aidigital.marketplace.wallet.api.dto.WalletView;
import com.aidigital.marketplace.wallet.application.WalletService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final WalletService walletService;

    public AdminUserController(AdminUserService adminUserService, WalletService walletService) {
        this.adminUserService = adminUserService;
        this.walletService = walletService;
    }

    @GetMapping
    public ListResponse<AdminUserView> list(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return adminUserService.list(username, status, page, pageSize);
    }

    @PostMapping("/{id}/status")
    public DataResponse<AdminUserView> status(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return new DataResponse<>(adminUserService.updateStatus(id, request.status()));
    }

    @PostMapping("/{id}/wallet-credits")
    public DataResponse<WalletView> credit(@PathVariable Long id, @Valid @RequestBody WalletCreditRequest request) {
        return new DataResponse<>(
                walletService.credit(id, request.amountFen(), "CREDIT", null, "管理员入账"));
    }
}
