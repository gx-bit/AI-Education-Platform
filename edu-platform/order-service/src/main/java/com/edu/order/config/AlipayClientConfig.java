package com.edu.order.config;

import com.alipay.api.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

@Configuration
@EnableConfigurationProperties(AlipayProperties.class)
public class AlipayClientConfig {
    @Bean
    @ConditionalOnProperty(prefix = "payment.alipay", name = "enabled", havingValue = "true")
    public AlipayClient alipayClient(AlipayProperties p) {
        return new DefaultAlipayClient(p.getGatewayUrl(), p.getAppId(), p.getMerchantPrivateKey(),
                "json", p.getCharset(), p.getAlipayPublicKey(), p.getSignType());
    }
}
