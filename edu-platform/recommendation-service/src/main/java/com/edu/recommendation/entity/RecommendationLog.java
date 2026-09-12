package com.edu.recommendation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_recommendation_log")
public class RecommendationLog {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private String requestId;
    private Long userId;
    private String sessionId;
    private Long courseId;
    private Integer rankPosition;
    private BigDecimal score;
    private String reason;
    private Integer clicked;
    private Integer purchased;
    private LocalDateTime createdAt;
}
