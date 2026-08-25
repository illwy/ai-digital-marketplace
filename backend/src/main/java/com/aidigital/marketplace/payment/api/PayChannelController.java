package com.aidigital.marketplace.payment.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.payment.api.dto.PayChannelView;
import com.aidigital.marketplace.payment.application.PaymentService;
import com.aidigital.marketplace.shared.web.DataResponse;

@RestController
@RequestMapping("/api/v1/payments")
public class PayChannelController {

    private final PaymentService paymentService;

    public PayChannelController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/channels")
    public DataResponse<PayChannelView> channels() {
        return new DataResponse<>(paymentService.channels());
    }
}
