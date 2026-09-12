package com.edu.order.service;

import com.edu.common.core.result.PageResult;
import com.edu.order.dto.CreateOrderRequest;
import com.edu.order.entity.Order;

import java.util.Map;

public interface OrderService {

    Order createOrder(Long userId, String username, CreateOrderRequest request);

    PageResult<Order> listOrders(Long userId, Integer status, int page, int size);

    Order getOrderById(Long orderId, Long userId);

    Order payOrder(Long orderId, Long userId);

    Order getOrderByOrderNo(String orderNo);

    boolean completeAlipayPayment(String orderNo, String providerTradeNo, java.math.BigDecimal paidAmount);

    Order completeMockPayment(Long orderId, Long userId);

    void cancelOrder(Long orderId, Long userId);

    PageResult<Order> listAllOrders(Integer status, int page, int size);

    Map<String, Object> getStats();

    /** 检查用户是否已支付过某一门课程 */
    boolean hasPaidCourse(Long userId, Long courseId);

    /** 通过课程ID支付（自动查找用户待支付订单） */
    Order payOrderByCourseId(Long userId, Long courseId);

    /** 通过课程ID取消待支付订单 */
    void cancelOrderByCourseId(Long userId, Long courseId);
}
