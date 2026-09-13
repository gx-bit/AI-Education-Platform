package com.edu.course.controller;

import com.edu.common.core.result.PageResult;
import com.edu.common.core.result.Result;
import com.edu.common.core.exception.BusinessException;
import com.edu.common.security.context.UserContext;
import com.edu.course.dto.*;
import com.edu.course.entity.Category;
import com.edu.course.service.CourseService;
import com.edu.course.service.CourseOutlineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * 课程控制器
 */
@Tag(name = "课程管理", description = "课程CRUD、分类、搜索、AI推荐")
@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseOutlineService courseOutlineService;

    @Operation(summary = "分页查询课程列表")
    @GetMapping("/list")
    public Result<PageResult<CourseVO>> listCourses(CourseQueryRequest query) {
        return Result.success(courseService.listCourses(query));
    }

    @Operation(summary = "获取课程详情")
    @GetMapping("/{id}")
    public Result<CourseVO> getCourse(@PathVariable("id") Long id) {
        return Result.success(courseService.getCourseById(id));
    }

    @Operation(summary = "根据课程链接自动整理课程目录")
    @GetMapping("/{id}/outline")
    public Result<CourseOutlineVO> getCourseOutline(@PathVariable("id") Long id) {
        return Result.success(courseOutlineService.extract(id));
    }

    @Operation(summary = "跳转到课程外部链接")
    @GetMapping("/{id}/goto")
    public void gotoCourseLink(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        CourseVO course = courseService.getCourseById(id);
        if (course == null || course.getLinkUrl() == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "课程或链接不存在");
            return;
        }
        response.sendRedirect(course.getLinkUrl());
    }

    @Operation(summary = "获取分类列表")
    @GetMapping("/category/list")
    public Result<List<Category>> listCategories() {
        return Result.success(courseService.listCategories());
    }

    @Operation(summary = "AI课程推荐")
    @GetMapping("/recommend")
    public Result<List<CourseVO>> recommend(
            @RequestParam(name = "interest", required = false) String interest,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "goal", required = false) String goal,
            @RequestParam(name = "limit", required = false, defaultValue = "3") Integer limit) {
        Long userId = UserContext.getCurrentUserId();
        List<CourseVO> recommendations = courseService.getAiRecommendations(userId, interest, level, goal, limit);
        return Result.success(recommendations);
    }

    // ===== 管理员/教师接口 =====

    @GetMapping("/admin/list")
    public Result<PageResult<CourseVO>> listAllCourses(CourseQueryRequest query) {
        requireCourseManager();
        return Result.success(courseService.listAllCourses(query));
    }

    @Operation(summary = "创建课程")
    @PostMapping
    public Result<CourseVO> createCourse(@Valid @RequestBody CourseRequest request) {
        requireCourseManager();
        Long userId = UserContext.getCurrentUserId();
        String username = UserContext.getCurrentUsername();
        String teacherName = UserContext.isAdmin() && request.getTeacherName() != null
                && !request.getTeacherName().isBlank() ? request.getTeacherName().trim() : username;
        CourseVO created = courseService.createCourse(request, userId, teacherName);
        if (UserContext.isAdmin()) {
            courseService.publishCourse(created.getId());
            created = courseService.getCourseById(created.getId());
        }
        return Result.success("创建成功", created);
    }

    @Operation(summary = "更新课程")
    @PutMapping("/{id}")
    public Result<CourseVO> updateCourse(
            @PathVariable("id") Long id,
            @Valid @RequestBody CourseRequest request) {
        requireCourseManager();
        return Result.success("更新成功", courseService.updateCourse(id, request));
    }

    @Operation(summary = "发布课程")
    @PutMapping("/{id}/publish")
    public Result<Void> publishCourse(@PathVariable("id") Long id) {
        requireCourseManager();
        courseService.publishCourse(id);
        return Result.success();
    }

    @Operation(summary = "下架课程")
    @PutMapping("/{id}/unpublish")
    public Result<Void> unpublishCourse(@PathVariable("id") Long id) {
        requireCourseManager();
        courseService.unpublishCourse(id);
        return Result.success();
    }

    @Operation(summary = "删除课程")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCourse(@PathVariable("id") Long id) {
        requireCourseManager();
        courseService.deleteCourse(id);
        return Result.success();
    }

    @Operation(summary = "增加学生数（内部接口）")
    @PutMapping("/{id}/student-count")
    public Result<Void> incrementStudentCount(@PathVariable("id") Long id) {
        courseService.incrementStudentCount(id);
        return Result.success();
    }

    private void requireCourseManager() {
        String role = UserContext.getCurrentRole();
        if (!"admin".equals(role) && !"teacher".equals(role)) {
            throw new BusinessException(403, "仅管理员或讲师可以管理课程");
        }
    }
}
