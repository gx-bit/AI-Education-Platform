package com.edu.course.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CourseOutlineVO {
    private String sourceUrl;
    private String sourceTitle;
    private String status;
    private String message;
    private List<String> items;
    private LocalDateTime fetchedAt;
}
