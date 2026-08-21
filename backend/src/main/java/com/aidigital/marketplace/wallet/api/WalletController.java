package com.aidigital.marketplace.wallet.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.identity.security.AuthUser;
import com.aidigital.marketplace.shared.web.DataResponse;
import com.aidigital.marketplace.wallet.api.dto.WalletCreditRequest;
import com.aidigital.marketplace.wallet.api.dto.WalletView;
import com.aidigital.marketplace.wallet.application.WalletService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public DataResponse<WalletView> me(@AuthenticationPrincipal AuthUser user) {
        walletService.createForUser(user.getId());
        return new DataResponse<>(walletService.get(user.getId()));
    }

    @PostMapping("/sandbox-credits")
    public DataResponse<WalletView> sandboxTopup(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody WalletCreditRequest request) {
        return new DataResponse<>(
                walletService.credit(user.getId(), request.amountFen(), "SANDBOX_TOPUP", null, "沙箱充值"));
    }
}
