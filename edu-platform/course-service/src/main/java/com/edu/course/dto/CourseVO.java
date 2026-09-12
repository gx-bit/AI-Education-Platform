package com.edu.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程视图对象
 */
@Data
@Schema(description = "课程信息")
public class CourseVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String title;
    private String description;
    private String coverImage;
    private String linkUrl;
    private Long teacherId;
    private String teacherName;
    private Long categoryId;
    private String categoryName;
    private BigDecimal price;
    private Integer duration;
    private String level;
    private String levelDesc;
    private Integer status;
    private Integer studentCount;
    private BigDecimal rating;
    private String tags;
    private LocalDateTime createdAt;

    /** AI推荐理由（AI推荐时填充） */
    private String recommendReason;
    private Integer matchScore;

    public String getLevelDesc() {
        return switch (this.level) {
            case "beginner" -> "入门";
            case "intermediate" -> "进阶";
            case "advanced" -> "高级";
            default -> this.level;
        };
    }
}
