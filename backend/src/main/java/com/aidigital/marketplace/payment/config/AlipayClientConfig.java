package com.aidigital.marketplace.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;

@Configuration
public class AlipayClientConfig {

    @Bean
    AlipayClient alipayClient(AlipayProperties properties) throws AlipayApiException {
        if (!properties.isConfigured()) {
            return null;
        }
        AlipayConfig config = new AlipayConfig();
        config.setServerUrl(properties.getServerUrl());
        config.setAppId(properties.getAppId());
        config.setPrivateKey(properties.getPrivateKey());
        config.setFormat("json");
        config.setAlipayPublicKey(properties.getAlipayPublicKey());
        config.setCharset("UTF-8");
        config.setSignType("RSA2");
        return new DefaultAlipayClient(config);
    }
}
