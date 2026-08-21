package com.aidigital.marketplace.payment.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/notify")
public class PaymentNotifyController {

    @PostMapping("/alipay")
    public String alipay() {
        return "success";
    }

    @PostMapping("/wechat")
    public String wechat() {
        return "SUCCESS";
    }
}
