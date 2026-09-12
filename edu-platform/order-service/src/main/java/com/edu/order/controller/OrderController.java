package com.edu.order.controller;

import com.edu.common.core.result.PageResult;
import com.edu.common.core.result.Result;
import com.edu.common.security.context.UserContext;
import com.edu.order.dto.CreateOrderRequest;
import com.edu.order.dto.PaymentFormResponse;
import com.edu.order.entity.Order;
import com.edu.order.service.AlipayPaymentService;
import com.edu.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;
import com.edu.common.core.exception.BusinessException;
import com.edu.common.core.result.ResultCode;

@Tag(name = "订单管理", description = "选课、订单、支付相关接口")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AlipayPaymentService alipayPaymentService;

    @Operation(summary = "创建订单（选课）")
    @PostMapping("/create")
    public Result<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Long userId = UserContext.getCurrentUserId();
        String username = UserContext.getCurrentUsername();
        return Result.success("选课成功", orderService.createOrder(userId, username, request));
    }

    @Operation(summary = "我的订单列表")
    @GetMapping("/list")
    public Result<PageResult<Order>> listOrders(
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(orderService.listOrders(userId, status, page, size));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{orderId}")
    public Result<Order> getOrder(@PathVariable("orderId") Long orderId) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(orderService.getOrderById(orderId, userId));
    }

    @Operation(summary = "模拟支付订单（通过订单ID）")
    @PostMapping("/{orderId}/pay")
    public Result<Order> payOrder(@PathVariable("orderId") Long orderId) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success("支付成功", orderService.payOrder(orderId, userId));
    }

    @Operation(summary = "创建支付宝电脑网站支付")
    @PostMapping("/{orderId}/payment/alipay")
    public Result<PaymentFormResponse> createAlipayPayment(@PathVariable("orderId") Long orderId) {
        return Result.success(alipayPaymentService.createPagePayment(orderId, UserContext.getCurrentUserId()));
    }

    @Operation(summary = "支付宝异步通知")
    @PostMapping(value = "/payment/alipay/notify", produces = "text/plain;charset=UTF-8")
    public String alipayNotify(HttpServletRequest request) {
        try {
            return alipayPaymentService.handleNotification(request.getParameterMap()) ? "success" : "fail";
        } catch (Exception e) {
            return "fail";
        }
    }

    @Operation(summary = "查询支付状态")
    @GetMapping("/{orderId}/payment/status")
    public Result<Order> paymentStatus(@PathVariable("orderId") Long orderId) {
        return Result.success(orderService.getOrderById(orderId, UserContext.getCurrentUserId()));
    }

    @Operation(summary = "取消订单（通过订单ID）")
    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancelOrder(@PathVariable("orderId") Long orderId) {
        Long userId = UserContext.getCurrentUserId();
        orderService.cancelOrder(orderId, userId);
        return Result.success();
    }

    @Operation(summary = "检查用户是否已购买某课程")
    @GetMapping("/check/{courseId}")
    public Result<Map<String, Object>> checkPurchased(@PathVariable("courseId") Long courseId) {
        Long userId = UserContext.getCurrentUserId();
        boolean paid = orderService.hasPaidCourse(userId, courseId);
        Map<String, Object> result = new HashMap<>();
        result.put("paid", paid);
        return Result.success(result);
    }

    @Operation(summary = "通过课程ID支付待支付订单")
    @PostMapping("/pay-by-course/{courseId}")
    public Result<Order> payByCourse(@PathVariable("courseId") Long courseId) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success("支付成功", orderService.payOrderByCourseId(userId, courseId));
    }

    @Operation(summary = "通过课程ID取消待支付订单")
    @PostMapping("/cancel-by-course/{courseId}")
    public Result<Void> cancelByCourse(@PathVariable("courseId") Long courseId) {
        Long userId = UserContext.getCurrentUserId();
        orderService.cancelOrderByCourseId(userId, courseId);
        return Result.success();
    }

    @Operation(summary = "所有订单列表（管理员）")
    @GetMapping("/admin/list")
    public Result<PageResult<Order>> listAllOrders(
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        requireAdmin();
        return Result.success(orderService.listAllOrders(status, page, size));
    }

    @Operation(summary = "订单统计数据（管理员）")
    @GetMapping("/admin/stats")
    public Result<Map<String, Object>> getStats() {
        requireAdmin();
        return Result.success(orderService.getStats());
    }

    private void requireAdmin() {
        if (!UserContext.isAdmin()) throw new BusinessException(ResultCode.FORBIDDEN);
    }
}
