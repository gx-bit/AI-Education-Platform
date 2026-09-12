package com.edu.course.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CourseReviewRequest {
    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    @Size(max = 1000)
    private String content;
}
