package com.edu.recommendation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_user_behavior")
public class UserBehavior {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long userId;
    private String sessionId;
    private Long courseId;
    private String behaviorType;
    private BigDecimal behaviorValue;
    private String contextJson;
    private LocalDateTime createdAt;
}
