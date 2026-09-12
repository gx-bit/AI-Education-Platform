package com.edu.recommendation.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RecommendationRequest {
    private String interest;
    @Pattern(regexp = "beginner|intermediate|advanced") private String level;
    private String goal;
    @Min(1) @Max(20) private Integer limit = 6;
    private String sessionId;
}
