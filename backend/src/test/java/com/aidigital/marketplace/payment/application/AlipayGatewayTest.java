package com.aidigital.marketplace.payment.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AlipayGatewayTest {

    @Test
    void fenToYuanKeepsTwoDecimals() {
        assertThat(AlipayGateway.fenToYuan(100)).isEqualTo("1.00");
        assertThat(AlipayGateway.fenToYuan(1)).isEqualTo("0.01");
        assertThat(AlipayGateway.fenToYuan(19900)).isEqualTo("199.00");
    }

    @Test
    void subjectStripsAlipayForbiddenChars() {
        assertThat(AlipayGateway.sanitizeSubject("A/B=C&D")).isEqualTo("A B C D");
        assertThat(AlipayGateway.sanitizeSubject("  ")).isEqualTo("钥市数字商品");
    }

    @Test
    void bodyIsTruncatedAndStripped() {
        assertThat(AlipayGateway.sanitizeBody("订单/1=2&3")).isEqualTo("订单 1 2 3");
        assertThat(AlipayGateway.sanitizeBody("x".repeat(200)).length()).isEqualTo(128);
    }
}
