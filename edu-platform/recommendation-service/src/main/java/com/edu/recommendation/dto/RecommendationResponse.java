package com.edu.recommendation.dto;

import lombok.*;
import java.util.List;

@Data @Builder
public class RecommendationResponse {
    private String requestId;
    private String strategy;
    private boolean personalized;
    private List<RecommendedCourse> courses;
}
