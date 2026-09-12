package com.edu.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_course_favorite")
public class CourseFavorite {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long courseId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
