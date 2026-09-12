package com.edu.course.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CourseReviewVO {
    private Long id;
    private Long userId;
    private String username;
    private Integer rating;
    private String content;
    private LocalDateTime updatedAt;
    private Boolean mine;
}
