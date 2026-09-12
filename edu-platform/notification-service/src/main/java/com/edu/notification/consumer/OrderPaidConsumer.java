package com.edu.notification.consumer;

import com.edu.notification.entity.Notification;
import com.edu.notification.mapper.NotificationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 订单消息消费者
 * 监听订单支付成功事件，发送站内通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidConsumer {

    private static final String QUEUE = "edu.order.paid.queue";

    private final NotificationMapper notificationMapper;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = QUEUE)
    public void handleOrderPaid(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // 解析消息
            Map<?, ?> event = objectMapper.readValue(message.getBody(), Map.class);
            Long userId = Long.parseLong(event.get("userId").toString());
            String username = (String) event.get("username");
            String courseTitle = (String) event.get("courseTitle");
            String orderNo = (String) event.get("orderNo");
            Object amountObj = event.get("amount");
            BigDecimal amount = amountObj != null ? new BigDecimal(amountObj.toString()) : BigDecimal.ZERO;

            log.info("收到订单支付事件: orderNo={}, userId={}, course={}", orderNo, userId, courseTitle);

            // 创建站内通知
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setTitle("选课成功通知");
            notification.setContent(String.format(
                    "您好，%s！您已成功购买课程《%s》，订单号：%s，金额：%.2f元。祝您学习愉快！",
                    username, courseTitle, orderNo, amount
            ));
            notification.setType("order");
            Object orderId = event.get("orderId");
            if (orderId != null) notification.setRelatedId(Long.parseLong(orderId.toString()));
            notification.setIsRead(0);
            notification.setCreatedAt(LocalDateTime.now());
            notificationMapper.insert(notification);

            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("通知发送成功，notificationId={}", notification.getId());

        } catch (Exception e) {
            log.error("处理订单支付消息失败", e);
            // 拒绝消息，放入死信队列（或重新入队）
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
