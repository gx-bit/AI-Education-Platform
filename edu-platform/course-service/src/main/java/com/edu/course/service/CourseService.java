package com.edu.course.service;

import com.edu.common.core.result.PageResult;
import com.edu.course.dto.*;
import com.edu.course.entity.Category;

import java.util.List;

/**
 * 课程服务接口
 */
public interface CourseService {

    PageResult<CourseVO> listCourses(CourseQueryRequest query);
    PageResult<CourseVO> listAllCourses(CourseQueryRequest query);

    CourseVO getCourseById(Long id);

    CourseVO createCourse(CourseRequest request, Long teacherId, String teacherName);

    CourseVO updateCourse(Long id, CourseRequest request);

    void deleteCourse(Long id);

    void publishCourse(Long id);

    void unpublishCourse(Long id);

    List<Category> listCategories();

    /** AI 推荐课程（调用 Claude API） */
    List<CourseVO> getAiRecommendations(Long userId, String userInterest, String level, String goal, Integer limit);

    /** 增加学生数量（订单支付成功后调用） */
    void incrementStudentCount(Long courseId);
}
