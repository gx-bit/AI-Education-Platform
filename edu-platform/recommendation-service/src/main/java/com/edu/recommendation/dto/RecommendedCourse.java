package com.edu.recommendation.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Data @Builder
public class RecommendedCourse {
    private Long id;
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
    private String recommendReason;
    private Integer matchScore;
    private Map<String, Integer> scoreDetails;
}
