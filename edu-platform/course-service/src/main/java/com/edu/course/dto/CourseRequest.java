package com.edu.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建/更新课程请求DTO
 */
@Data
@Schema(description = "课程创建/更新请求")
public class CourseRequest {

    @NotBlank(message = "课程标题不能为空")
    @Schema(description = "课程标题")
    private String title;

    @Schema(description = "课程描述")
    private String description;

    @Schema(description = "课程封面图URL")
    private String coverImage;

    @Schema(description = "课程学习链接")
    private String linkUrl;

    @Schema(description = "讲师显示名称")
    private String teacherName;

    @NotNull(message = "分类ID不能为空")
    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "价格（0为免费）")
    private BigDecimal price = BigDecimal.ZERO;

    @Schema(description = "课程时长（分钟）")
    private Integer duration;

    @Schema(description = "难度：beginner/intermediate/advanced")
    private String level = "beginner";

    @Schema(description = "标签（逗号分隔）")
    private String tags;
}
