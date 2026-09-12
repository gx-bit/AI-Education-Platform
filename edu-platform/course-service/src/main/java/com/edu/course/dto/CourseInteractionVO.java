package com.edu.course.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CourseInteractionVO {
    private Boolean favorite;
    private Long favoriteCount;
    private Long reviewCount;
    private BigDecimal averageRating;
    private CourseReviewVO myReview;
}
