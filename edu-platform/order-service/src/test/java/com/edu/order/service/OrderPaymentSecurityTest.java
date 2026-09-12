package com.edu.order.service;

import com.edu.common.core.exception.BusinessException;
import com.edu.order.client.CourseFeign;
import com.edu.order.entity.Order;
import com.edu.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderPaymentSecurityTest {
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new OrderServiceImpl(mock(CourseFeign.class), mock(RabbitTemplate.class)));
    }

    @Test
    void userCannotReadAnotherUsersOrder() {
        Order order = order(10L, 7L, "99.00", 0);
        doReturn(order).when(service).getById(10L);
        assertThrows(BusinessException.class, () -> service.getOrderById(10L, 8L));
    }

    @Test
    void paidCourseCannotUseLegacySimulationEndpoint() {
        Order order = order(10L, 7L, "99.00", 0);
        doReturn(order).when(service).getById(10L);
        assertThrows(BusinessException.class, () -> service.payOrder(10L, 7L));
    }

    @Test
    void duplicateVerifiedNotificationIsIdempotent() {
        Order order = order(10L, 7L, "99.00", 1);
        doReturn(order).when(service).getOrderByOrderNo("ORDER-1");
        assertTrue(service.completeAlipayPayment("ORDER-1", "TRADE-1", new BigDecimal("99.00")));
        verify(service, never()).update(any());
    }

    @Test
    void callbackAmountMustMatchOrderSnapshot() {
        Order order = order(10L, 7L, "99.00", 0);
        doReturn(order).when(service).getOrderByOrderNo("ORDER-1");
        assertThrows(BusinessException.class,
                () -> service.completeAlipayPayment("ORDER-1", "TRADE-1", new BigDecimal("0.01")));
    }

    private Order order(Long id, Long userId, String amount, int status) {
        Order order = new Order();
        order.setId(id); order.setOrderNo("ORDER-1"); order.setUserId(userId);
        order.setAmount(new BigDecimal(amount)); order.setStatus(status);
        return order;
    }
}
