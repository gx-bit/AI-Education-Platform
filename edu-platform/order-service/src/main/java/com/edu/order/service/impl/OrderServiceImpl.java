package com.edu.order.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.edu.common.core.exception.BusinessException;
import com.edu.common.core.result.PageResult;
import com.edu.common.core.result.Result;
import com.edu.common.core.result.ResultCode;
import com.edu.order.client.CourseFeign;
import com.edu.order.config.RabbitMQConfig;
import com.edu.order.dto.CreateOrderRequest;
import com.edu.order.entity.Order;
import com.edu.order.mapper.OrderMapper;
import com.edu.order.mq.OrderPaidEvent;
import com.edu.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final CourseFeign courseFeign;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long userId, String username, CreateOrderRequest request) {
        // 查询是否已有未支付或已支付订单
        Order existOrder = getOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getCourseId, request.getCourseId())
                .in(Order::getStatus, 0, 1)
                .last("limit 1"));

        if (existOrder != null) {
            if (existOrder.getStatus() == 0) {
                log.info("已有未支付订单，直接返回: orderNo={}", existOrder.getOrderNo());
                return existOrder;
            } else {
                throw new BusinessException(ResultCode.ORDER_ALREADY_EXISTS);
            }
        }

        // 调用课程服务获取课程信息
        Result<Map<String, Object>> courseResult = courseFeign.getCourseById(request.getCourseId());
        if (!courseResult.isSuccess() || courseResult.getData() == null) {
            throw new BusinessException(ResultCode.COURSE_NOT_FOUND);
        }

        Map<String, Object> courseData = courseResult.getData();
        String courseTitle = (String) courseData.get("title");
        String courseCover = (String) courseData.get("coverImage");
        Object priceObj = courseData.get("price");
        BigDecimal price = priceObj != null ? new BigDecimal(priceObj.toString()) : BigDecimal.ZERO;

        Order order = new Order();
        order.setOrderNo(IdUtil.getSnowflakeNextIdStr());
        order.setUserId(userId);
        order.setUsername(username);
        order.setCourseId(request.getCourseId());
        order.setCourseTitle(courseTitle);
        order.setCourseCover(courseCover);
        order.setAmount(price);
        order.setStatus(0);
        order.setPayMethod(request.getPayMethod());

        save(order);
        log.info("订单创建成功: orderNo={}, userId={}, courseId={}", order.getOrderNo(), userId, request.getCourseId());
        return order;
    }

    @Override
    public Order getOrderByOrderNo(String orderNo) {
        Order order = getOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo).last("limit 1"));
        if (order == null) throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        return order;
    }

    @Override
    public PageResult<Order> listOrders(Long userId, Integer status, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        Page<Order> pageResult = page(new Page<>(page, size), wrapper);
        return PageResult.of(pageResult.getCurrent(), pageResult.getSize(),
                pageResult.getTotal(), pageResult.getRecords());
    }

    @Override
    public Order getOrderById(Long orderId, Long userId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (userId != null && !userId.equals(order.getUserId())) {
            throw new BusinessException("无权访问该订单");
        }
        // 不校验 userId，直接返回
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order payOrder(Long orderId, Long userId) {
        Order order = getOrderById(orderId, userId);
        if (order.getAmount() != null && order.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("付费订单必须通过支付宝支付");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        order.setStatus(1);
        order.setPaidAt(LocalDateTime.now());
        order.setPayMethod("free");
        order.setProviderTradeNo("FREE-" + order.getOrderNo());
        updateById(order);

        // 通知课程服务更新学生数
        try {
            courseFeign.incrementStudentCount(order.getCourseId());
        } catch (Exception e) {
            log.warn("更新课程学生数失败，将异步重试: {}", e.getMessage());
        }

        // 发送支付成功消息
        OrderPaidEvent event = OrderPaidEvent.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .username(order.getUsername())
                .courseId(order.getCourseId())
                .courseTitle(order.getCourseTitle())
                .amount(order.getAmount())
                .paidAt(order.getPaidAt())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_PAID_ROUTING_KEY,
                event
        );
        log.info("订单支付成功，消息已发送: orderNo={}", order.getOrderNo());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeAlipayPayment(String orderNo, String providerTradeNo, BigDecimal paidAmount) {
        Order order = getOrderByOrderNo(orderNo);
        if (order.getAmount() == null || order.getAmount().compareTo(paidAmount) != 0) {
            throw new BusinessException("支付金额与订单金额不一致");
        }
        if (order.getStatus() == 1) return true;
        if (order.getStatus() != 0) throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);

        LocalDateTime paidAt = LocalDateTime.now();
        boolean changed = update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, order.getId()).eq(Order::getStatus, 0)
                .set(Order::getStatus, 1).set(Order::getPaidAt, paidAt)
                .set(Order::getPayMethod, "alipay").set(Order::getProviderTradeNo, providerTradeNo));
        if (!changed) return getById(order.getId()).getStatus() == 1;
        order.setStatus(1); order.setPaidAt(paidAt); order.setPayMethod("alipay"); order.setProviderTradeNo(providerTradeNo);

        try { courseFeign.incrementStudentCount(order.getCourseId()); }
        catch (Exception e) { log.warn("更新课程学员数失败: {}", e.getMessage()); }
        OrderPaidEvent event = OrderPaidEvent.builder()
                .orderId(order.getId()).orderNo(order.getOrderNo()).userId(order.getUserId())
                .username(order.getUsername()).courseId(order.getCourseId()).courseTitle(order.getCourseTitle())
                .amount(order.getAmount()).paidAt(paidAt).build();
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_PAID_ROUTING_KEY, event);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId) {
        Order order = getOrderById(orderId, userId);
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
        order.setStatus(2);
        updateById(order);
    }

    @Override
    public PageResult<Order> listAllOrders(Integer status, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .orderByDesc(Order::getCreatedAt);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        Page<Order> pageResult = page(new Page<>(page, size), wrapper);
        return PageResult.of(pageResult.getCurrent(), pageResult.getSize(),
                pageResult.getTotal(), pageResult.getRecords());
    }

    @Override
    public Map<String, Object> getStats() {
        long totalOrders = count();
        long pendingOrders = count(new LambdaQueryWrapper<Order>().eq(Order::getStatus, 0));
        long paidOrders = count(new LambdaQueryWrapper<Order>().eq(Order::getStatus, 1));
        long cancelledOrders = count(new LambdaQueryWrapper<Order>().eq(Order::getStatus, 2));
        long refundedOrders = count(new LambdaQueryWrapper<Order>().eq(Order::getStatus, 3));

        List<Order> paidList = list(new LambdaQueryWrapper<Order>().eq(Order::getStatus, 1));
        BigDecimal totalRevenue = paidList.stream()
                .map(Order::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "totalOrders", totalOrders,
                "totalRevenue", totalRevenue,
                "pendingOrders", pendingOrders,
                "paidOrders", paidOrders,
                "cancelledOrders", cancelledOrders,
                "refundedOrders", refundedOrders
        );
    }

    @Override
    public boolean hasPaidCourse(Long userId, Long courseId) {
        return count(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getCourseId, courseId)
                .eq(Order::getStatus, 1)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order payOrderByCourseId(Long userId, Long courseId) {
        Order order = getOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getCourseId, courseId)
                .eq(Order::getStatus, 0)
                .last("limit 1"));
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return payOrder(order.getId(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrderByCourseId(Long userId, Long courseId) {
        Order order = getOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getCourseId, courseId)
                .eq(Order::getStatus, 0)
                .last("limit 1"));
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        cancelOrder(order.getId(), userId);
    }
}
