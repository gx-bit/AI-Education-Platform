package com.edu.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.edu.common.core.exception.BusinessException;
import com.edu.common.core.result.PageResult;
import com.edu.common.core.result.ResultCode;
import com.edu.common.core.util.BeanCopyUtil;
import com.edu.course.client.ClaudeAiClient;
import com.edu.course.dto.CourseQueryRequest;
import com.edu.course.dto.CourseRequest;
import com.edu.course.dto.CourseVO;
import com.edu.course.entity.Category;
import com.edu.course.entity.Course;
import com.edu.course.mapper.CategoryMapper;
import com.edu.course.mapper.CourseMapper;
import com.edu.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final CourseMapper courseMapper;
    private final CategoryMapper categoryMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ClaudeAiClient claudeAiClient;

    private static final String COURSE_CACHE_PREFIX = "course:detail:";
    private static final String CATEGORY_CACHE_KEY = "course:categories";
    private static final long CACHE_TTL_MINUTES = 30;

    @Value("${ai.claude.api-key:}")
    private String claudeApiKey;

    @Override
    public PageResult<CourseVO> listCourses(CourseQueryRequest query) {
        Page<CourseVO> page = new Page<>(query.getPage(), query.getSize());
        Page<CourseVO> resultPage = (Page<CourseVO>) courseMapper.selectCourseVOPage(page, query);
        return PageResult.of(
                resultPage.getCurrent(), resultPage.getSize(),
                resultPage.getTotal(), resultPage.getRecords());
    }

    @Override
    @SuppressWarnings("unchecked")
    public CourseVO getCourseById(Long id) {
        String cacheKey = COURSE_CACHE_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof CourseVO vo) {
            return vo;
        }

        Course course = getById(id);
        if (course == null) {
            throw new BusinessException(ResultCode.COURSE_NOT_FOUND);
        }

        CourseVO vo = BeanCopyUtil.copyBean(course, CourseVO.class);
        if (course.getCategoryId() != null) {
            Category category = categoryMapper.selectById(course.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        redisTemplate.opsForValue().set(cacheKey, vo, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseVO createCourse(CourseRequest request, Long teacherId, String teacherName) {
        Course course = BeanCopyUtil.copyBean(request, Course.class);
        course.setTeacherId(teacherId);
        course.setTeacherName(teacherName);
        course.setStatus(0);
        course.setStudentCount(0);
        save(course);
        return getCourseById(course.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseVO updateCourse(Long id, CourseRequest request) {
        Course course = getById(id);
        if (course == null) {
            throw new BusinessException(ResultCode.COURSE_NOT_FOUND);
        }

        Course update = BeanCopyUtil.copyBean(request, Course.class);
        update.setId(id);
        updateById(update);
        redisTemplate.delete(COURSE_CACHE_PREFIX + id);
        return getCourseById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourse(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(ResultCode.COURSE_NOT_FOUND);
        }
        redisTemplate.delete(COURSE_CACHE_PREFIX + id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishCourse(Long id) {
        Course course = getById(id);
        if (course == null) {
            throw new BusinessException(ResultCode.COURSE_NOT_FOUND);
        }
        course.setStatus(1);
        updateById(course);
        redisTemplate.delete(COURSE_CACHE_PREFIX + id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpublishCourse(Long id) {
        Course course = getById(id);
        if (course == null) {
            throw new BusinessException(ResultCode.COURSE_NOT_FOUND);
        }
        course.setStatus(2);
        updateById(course);
        redisTemplate.delete(COURSE_CACHE_PREFIX + id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Category> listCategories() {
        Object cached = redisTemplate.opsForValue().get(CATEGORY_CACHE_KEY);
        if (cached instanceof List) {
            return (List<Category>) cached;
        }

        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort));
        redisTemplate.opsForValue().set(CATEGORY_CACHE_KEY, categories, 60, TimeUnit.MINUTES);
        return categories;
    }

    @Override
    public List<CourseVO> getAiRecommendations(Long userId, String userInterest, String level, String goal, Integer limit) {
        int recommendationLimit = limit == null ? 3 : Math.max(1, Math.min(limit, 10));

        CourseQueryRequest query = new CourseQueryRequest();
        query.setOrderBy("student_count");
        query.setPage(1);
        query.setSize(20);
        PageResult<CourseVO> hotCourses = listCourses(query);

        if (hotCourses.getRecords().isEmpty()) {
            return List.of();
        }

        if (shouldUseMockRecommendation()) {
            return buildMockRecommendations(hotCourses.getRecords(), userInterest, level, goal, recommendationLimit);
        }

        String courseSummary = hotCourses.getRecords().stream()
                .map(c -> String.format("ID:%d Title:%s Category:%s Level:%s Price:%s",
                        c.getId(), c.getTitle(), c.getCategoryName(), c.getLevelDesc(), c.getPrice()))
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
                You are an online learning advisor. Recommend courses only from this catalog:
                %s

                Learner interest: %s
                Current level: %s
                Learning goal: %s

                Ranking priority (strict): learning interest 45%%, learning goal 40%%, current level 5%%,
                course quality and popularity 10%%. Never rank an unrelated course first merely because its level matches.

                Return exactly %d recommendations as JSON only:
                [{"id": 1, "reason": "reason within 50 Chinese characters", "matchScore": 95}]
                """,
                courseSummary,
                hasText(userInterest) ? userInterest : "general skill improvement",
                hasText(level) ? level : "beginner",
                hasText(goal) ? goal : "finish a practical learning path",
                recommendationLimit);

        try {
            List<CourseVO> aiRecommendations = claudeAiClient.parseRecommendations(
                    claudeAiClient.chat(prompt), hotCourses.getRecords());
            if (aiRecommendations.isEmpty()) {
                return buildMockRecommendations(hotCourses.getRecords(), userInterest, level, goal, recommendationLimit);
            }
            return aiRecommendations.stream().limit(recommendationLimit).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("AI recommendation failed, using local mock recommendations: {}", e.getMessage());
            return buildMockRecommendations(hotCourses.getRecords(), userInterest, level, goal, recommendationLimit);
        }
    }

    private boolean shouldUseMockRecommendation() {
        return !hasText(claudeApiKey)
                || claudeApiKey.contains("placeholder")
                || claudeApiKey.contains("your-api-key");
    }

    private List<CourseVO> buildMockRecommendations(List<CourseVO> courses, String interest, String level, String goal, int limit) {
        String normalizedInterest = safe(interest).toLowerCase();
        String normalizedGoal = safe(goal).toLowerCase();
        String normalizedLevel = safe(level);

        return courses.stream()
                .peek(course -> {
                    int score = 60;
                    String haystack = String.join(" ",
                            safe(course.getTitle()),
                            safe(course.getDescription()),
                            safe(course.getCategoryName()),
                            safe(course.getTags())).toLowerCase();

                    if (hasText(normalizedInterest) && haystack.contains(normalizedInterest)) {
                        score += 18;
                    }
                    if (hasText(normalizedLevel) && normalizedLevel.equals(course.getLevel())) {
                        score += 3;
                    }
                    if (hasText(normalizedGoal) && haystack.contains(normalizedGoal)) {
                        score += 17;
                    }
                    score += Math.min(3, course.getStudentCount() == null ? 0 : course.getStudentCount() / 2000);

                    course.setMatchScore(Math.min(score, 99));
                    course.setRecommendReason(buildReason(interest, level, goal));
                })
                .sorted((a, b) -> Integer.compare(
                        b.getMatchScore() == null ? 0 : b.getMatchScore(),
                        a.getMatchScore() == null ? 0 : a.getMatchScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private String buildReason(String interest, String level, String goal) {
        StringBuilder reason = new StringBuilder("匹配课程库中的热门课程");
        if (hasText(interest)) {
            reason.append("，贴合兴趣：").append(interest);
        }
        if (hasText(level)) {
            reason.append("，适合当前水平：").append(level);
        }
        if (hasText(goal)) {
            reason.append("，服务目标：").append(goal);
        }
        return reason.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementStudentCount(Long courseId) {
        update(new LambdaUpdateWrapper<Course>()
                .eq(Course::getId, courseId)
                .setSql("student_count = student_count + 1"));
        redisTemplate.delete(COURSE_CACHE_PREFIX + courseId);
    }
}
