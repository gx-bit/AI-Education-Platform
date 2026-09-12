package com.edu.order.service;

import com.alipay.api.*;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.internal.util.AlipaySignature;
import com.edu.common.core.exception.BusinessException;
import com.edu.order.config.AlipayProperties;
import com.edu.order.dto.PaymentFormResponse;
import com.edu.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayPaymentService {
    private final ObjectProvider<AlipayClient> clientProvider;
    private final AlipayProperties properties;
    private final OrderService orderService;

    public PaymentFormResponse createPagePayment(Long orderId, Long userId) {
        Order order = orderService.getOrderById(orderId, userId);
        if (order.getStatus() == 1) throw new BusinessException("订单已支付");
        if (order.getStatus() != 0) throw new BusinessException("订单状态不允许支付");
        if (order.getAmount() == null || order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("零金额订单无需调用支付宝");
        }

        if (!isConfigured()) {
            if (!properties.isMockEnabled()) {
                throw new BusinessException("支付宝配置不完整：请配置商户参数，或在开发环境启用本地沙箱收银台");
            }
            String url = properties.getMockCashierUrl()
                    + "?orderId=" + order.getId()
                    + "&orderNo=" + encode(order.getOrderNo())
                    + "&amount=" + encode(order.getAmount().setScale(2).toPlainString())
                    + "&title=" + encode(order.getCourseTitle());
            return new PaymentFormResponse(order.getOrderNo(), "mock", null, url);
        }

        AlipayClient client = requireClient();

        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(order.getOrderNo());
        model.setTotalAmount(order.getAmount().setScale(2).toPlainString());
        model.setSubject(limit("课程购买：" + order.getCourseTitle(), 256));
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        model.setTimeoutExpress(properties.getTimeoutExpress());

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setBizModel(model);
        request.setNotifyUrl(properties.getNotifyUrl());
        request.setReturnUrl(properties.getReturnUrl());
        try {
            return new PaymentFormResponse(order.getOrderNo(), "alipay", client.pageExecute(request).getBody(), null);
        } catch (AlipayApiException e) {
            log.error("创建支付宝支付失败, orderNo={}", order.getOrderNo(), e);
            throw new BusinessException("创建支付宝支付失败，请稍后重试");
        }
    }

    public Order confirmMockPayment(Long orderId, Long userId) {
        if (!properties.isMockEnabled()) {
            throw new BusinessException("本地沙箱支付未启用");
        }
        return orderService.payOrder(orderId, userId);
    }

    public boolean handleNotification(Map<String, String[]> requestParameters) {
        requireConfigured();
        Map<String, String> params = new HashMap<>();
        requestParameters.forEach((key, values) -> params.put(key, String.join(",", values)));
        try {
            if (!AlipaySignature.rsaCheckV1(params, properties.getAlipayPublicKey(),
                    properties.getCharset(), properties.getSignType())) {
                log.warn("支付宝回调验签失败, outTradeNo={}", params.get("out_trade_no"));
                return false;
            }
        } catch (AlipayApiException e) {
            log.warn("支付宝回调验签异常", e);
            return false;
        }

        String tradeStatus = params.get("trade_status");
        if (!Set.of("TRADE_SUCCESS", "TRADE_FINISHED").contains(tradeStatus)) return true;
        if (!Objects.equals(properties.getAppId(), params.get("app_id"))) return false;
        if (hasText(properties.getSellerId()) && !Objects.equals(properties.getSellerId(), params.get("seller_id"))) return false;
        String orderNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String amount = params.get("total_amount");
        if (!hasText(orderNo) || !hasText(tradeNo) || !hasText(amount)) return false;
        return orderService.completeAlipayPayment(orderNo, tradeNo, new BigDecimal(amount));
    }

    private AlipayClient requireClient() {
        requireConfigured();
        AlipayClient client = clientProvider.getIfAvailable();
        if (client == null) throw new BusinessException("支付宝支付未启用");
        return client;
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new BusinessException("支付宝配置不完整，请配置沙箱或生产商户参数");
        }
    }

    private boolean isConfigured() {
        return properties.isEnabled() && hasText(properties.getAppId()) &&
                hasText(properties.getMerchantPrivateKey()) && hasText(properties.getAlipayPublicKey()) &&
                hasText(properties.getNotifyUrl());
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String encode(String value) { return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8); }
    private String limit(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }
}
