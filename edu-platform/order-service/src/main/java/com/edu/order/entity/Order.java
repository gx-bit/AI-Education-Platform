package com.edu.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@TableName("t_order")
public class Order implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单号（唯一，业务展示用） */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 课程ID */
    private Long courseId;

    /** 课程标题（快照） */
    private String courseTitle;

    /** 课程封面（快照） */
    private String courseCover;

    /** 支付金额 */
    private BigDecimal amount;

    /** 状态：0-待支付 1-已支付 2-已取消 3-已退款 */
    private Integer status;

    /** 支付时间 */
    private LocalDateTime paidAt;

    /** 支付方式（模拟：alipay/wechat） */
    private String payMethod;

    /** 支付宝交易号，用于对账和退款。 */
    private String providerTradeNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
