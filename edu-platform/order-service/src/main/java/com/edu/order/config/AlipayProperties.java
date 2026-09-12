package com.edu.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "payment.alipay")
public class AlipayProperties {
    private boolean enabled;
    /** Development-only cashier used when real Alipay credentials are unavailable. */
    private boolean mockEnabled;
    private String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";
    private String appId;
    private String merchantPrivateKey;
    private String alipayPublicKey;
    private String sellerId;
    private String notifyUrl;
    private String returnUrl = "http://localhost/payment/result";
    private String mockCashierUrl = "http://localhost/payment/cashier";
    private String charset = "UTF-8";
    private String signType = "RSA2";
    private String timeoutExpress = "30m";
}
