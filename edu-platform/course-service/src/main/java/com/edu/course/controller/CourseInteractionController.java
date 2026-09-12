package com.edu.course.controller;

import com.edu.common.core.result.*;
import com.edu.common.security.context.UserContext;
import com.edu.course.dto.*;
import com.edu.course.service.CourseInteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "课程互动", description = "课程收藏、评价与互动统计")
@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseInteractionController {
    private final CourseInteractionService service;

    @Operation(summary = "获取课程互动状态")
    @GetMapping("/{courseId}/interaction")
    public Result<CourseInteractionVO> interaction(@PathVariable("courseId") Long courseId) {
        return Result.success(service.getInteraction(courseId, UserContext.getCurrentUserId()));
    }

    @Operation(summary = "收藏课程")
    @PostMapping("/{courseId}/favorite")
    public Result<Void> favorite(@PathVariable("courseId") Long courseId) {
        service.favorite(courseId, UserContext.getCurrentUserId());
        return Result.success();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{courseId}/favorite")
    public Result<Void> unfavorite(@PathVariable("courseId") Long courseId) {
        service.unfavorite(courseId, UserContext.getCurrentUserId());
        return Result.success();
    }

    @Operation(summary = "我的收藏")
    @GetMapping("/favorites")
    public Result<PageResult<CourseVO>> favorites(
            @RequestParam(name = "page", defaultValue = "1") long page,
            @RequestParam(name = "size", defaultValue = "12") long size) {
        return Result.success(service.listFavorites(UserContext.getCurrentUserId(), page, size));
    }

    @Operation(summary = "课程评价列表")
    @GetMapping("/{courseId}/reviews")
    public Result<PageResult<CourseReviewVO>> reviews(
            @PathVariable("courseId") Long courseId,
            @RequestParam(name = "page", defaultValue = "1") long page,
            @RequestParam(name = "size", defaultValue = "10") long size) {
        return Result.success(service.listReviews(courseId, UserContext.getCurrentUserId(), page, size));
    }

    @Operation(summary = "发表或修改我的评价")
    @PostMapping("/{courseId}/reviews")
    public Result<CourseReviewVO> review(@PathVariable("courseId") Long courseId,
                                         @Valid @RequestBody CourseReviewRequest request) {
        return Result.success("评价已保存", service.review(courseId, UserContext.getCurrentUserId(),
                UserContext.getCurrentUsername(), request));
    }
}
