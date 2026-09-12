package com.edu.recommendation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("t_course")
public class CourseSnapshot {
    @TableId private Long id;
    private String title;
    private String description;
    private String coverImage;
    private String linkUrl;
    private Long teacherId;
    private String teacherName;
    private Long categoryId;
    private BigDecimal price;
    private Integer duration;
    private String level;
    private Integer status;
    private Integer studentCount;
    private BigDecimal rating;
    private String tags;
    @TableLogic private Integer deleted;
}
