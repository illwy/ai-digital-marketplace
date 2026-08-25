package com.aidigital.marketplace.payment.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aidigital.marketplace.payment.application.PaymentService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/payments/notify")
public class PaymentNotifyController {

    private final PaymentService paymentService;

    public PaymentNotifyController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/alipay")
    public String alipay(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return paymentService.handleAlipayNotify(params);
    }

    @PostMapping("/wechat")
    public String wechat() {
        return "SUCCESS";
    }
}
