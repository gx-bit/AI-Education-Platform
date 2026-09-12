package com.edu.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_course_review")
public class CourseReview {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String username;
    private Long courseId;
    private Integer rating;
    private String content;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
