package com.edu.recommendation.dto;

import lombok.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.util.Map;

@Data @Builder
public class RecommendedCourse {
    @JsonSerialize(using = ToStringSerializer.class)
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
