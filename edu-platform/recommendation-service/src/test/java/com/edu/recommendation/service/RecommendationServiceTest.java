package com.edu.recommendation.service;

import com.edu.recommendation.dto.*;
import com.edu.recommendation.entity.*;
import com.edu.recommendation.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecommendationServiceTest {
    private CourseSnapshotMapper courseMapper;
    private UserBehaviorMapper behaviorMapper;
    private RecommendationLogMapper logMapper;
    private RecommendationService service;

    @BeforeEach
    void setUp() {
        courseMapper = mock(CourseSnapshotMapper.class);
        behaviorMapper = mock(UserBehaviorMapper.class);
        logMapper = mock(RecommendationLogMapper.class);
        service = new RecommendationService(courseMapper, behaviorMapper, logMapper);
        ReflectionTestUtils.setField(service, "interestWeight", .36);
        ReflectionTestUtils.setField(service, "goalWeight", .34);
        ReflectionTestUtils.setField(service, "profileWeight", .12);
        ReflectionTestUtils.setField(service, "popularityWeight", .10);
        ReflectionTestUtils.setField(service, "qualityWeight", .08);
        ReflectionTestUtils.setField(service, "levelBonus", .03);
        when(logMapper.insert(any(RecommendationLog.class))).thenReturn(1);
    }

    @Test
    void semanticIntentChangesRankingAndCreatesExposureLogs() {
        when(courseMapper.selectList(any())).thenReturn(List.of(
                course(1L, "Spring Boot Java 微服务", "Java 后端开发", "Java,Spring", "beginner", 100, "4.8"),
                course(2L, "Python 数据分析", "Pandas 可视化", "Python,数据", "beginner", 100, "4.8")));

        RecommendationRequest request = new RecommendationRequest();
        request.setInterest("Java 后端 Spring"); request.setLevel("beginner"); request.setLimit(1);
        RecommendationResponse response = service.recommend(null, request);

        assertEquals(1L, response.getCourses().get(0).getId());
        assertFalse(response.isPersonalized());
        assertNotNull(response.getRequestId());
        verify(logMapper, times(1)).insert(any(RecommendationLog.class));
    }

    @Test
    void purchasedCoursesAreExcludedForKnownUser() {
        CourseSnapshot purchased = course(1L, "Java 入门", "Java", "Java", "beginner", 100, "5.0");
        CourseSnapshot alternative = course(2L, "Spring 进阶", "Spring Java", "Java,Spring", "intermediate", 80, "4.7");
        when(courseMapper.selectList(any())).thenReturn(List.of(purchased, alternative));
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(7L); behavior.setCourseId(1L); behavior.setBehaviorType("purchase");
        when(behaviorMapper.selectList(any())).thenReturn(List.of(behavior));

        RecommendationRequest request = new RecommendationRequest();
        request.setInterest("Java"); request.setLimit(2);
        RecommendationResponse response = service.recommend(7L, request);

        assertTrue(response.isPersonalized());
        assertEquals(List.of(2L), response.getCourses().stream().map(RecommendedCourse::getId).toList());
    }

    @Test
    void interestAndGoalOutrankLevelOnlyMatch() {
        when(courseMapper.selectList(any())).thenReturn(List.of(
                course(1L, "Java Spring 微服务实战", "构建后端求职项目", "Java,Spring,微服务", "intermediate", 100, "4.8"),
                course(2L, "摄影基础", "相机构图与后期", "摄影,艺术", "beginner", 100, "4.8")));

        RecommendationRequest request = new RecommendationRequest();
        request.setInterest("Java Spring");
        request.setGoal("微服务后端求职项目");
        request.setLevel("beginner");
        request.setLimit(1);

        RecommendationResponse response = service.recommend(null, request);

        assertEquals(1L, response.getCourses().get(0).getId());
        assertEquals("intent-first-interest-goal-v2", response.getStrategy());
    }

    private CourseSnapshot course(Long id, String title, String description, String tags, String level, int students, String rating) {
        CourseSnapshot c = new CourseSnapshot();
        c.setId(id); c.setTitle(title); c.setDescription(description); c.setTags(tags); c.setLevel(level);
        c.setStatus(1); c.setCategoryId(id); c.setStudentCount(students); c.setRating(new BigDecimal(rating));
        return c;
    }
}
