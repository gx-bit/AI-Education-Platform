package com.edu.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内通知实体
 */
@Data
@TableName("t_notification")
public class Notification implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 接收用户ID */
    private Long userId;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 类型：system/order/course */
    private String type;

    /** 是否已读：0-未读 1-已读 */
    private Integer isRead;

    /** 关联业务ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long relatedId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
