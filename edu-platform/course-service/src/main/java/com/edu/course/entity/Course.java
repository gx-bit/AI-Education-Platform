package com.edu.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程实体
 */
@Data
@TableName("t_course")
public class Course implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 课程标题 */
    private String title;

    /** 课程描述 */
    private String description;

    /** 课程封面图 */
    private String coverImage;
    @TableField("link_url")
    private String linkUrl;

    /** 讲师ID */
    private Long teacherId;

    /** 讲师姓名 */
    private String teacherName;

    /** 分类ID */
    private Long categoryId;

    /** 价格（0表示免费） */
    private BigDecimal price;

    /** 课程时长（分钟） */
    private Integer duration;

    /** 难度：beginner/intermediate/advanced */
    private String level;

    /** 状态：0-草稿 1-已发布 2-下架 */
    private Integer status;

    /** 学生人数 */
    private Integer studentCount;

    /** 评分（1-5） */
    private BigDecimal rating;

    /** 标签（逗号分隔） */
    private String tags;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
