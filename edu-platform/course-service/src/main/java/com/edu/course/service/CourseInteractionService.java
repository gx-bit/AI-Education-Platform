package com.edu.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.common.core.exception.BusinessException;
import com.edu.common.core.result.PageResult;
import com.edu.course.dto.*;
import com.edu.course.entity.*;
import com.edu.course.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseInteractionService {
    private final CourseMapper courseMapper;
    private final CourseFavoriteMapper favoriteMapper;
    private final CourseReviewMapper reviewMapper;

    public CourseInteractionVO getInteraction(Long courseId, Long userId) {
        requireCourse(courseId);
        Long favoriteCount = favoriteMapper.selectCount(new LambdaQueryWrapper<CourseFavorite>()
                .eq(CourseFavorite::getCourseId, courseId));
        List<CourseReview> reviews = reviewMapper.selectList(new LambdaQueryWrapper<CourseReview>()
                .eq(CourseReview::getCourseId, courseId));
        CourseReview mine = userId == null ? null : reviews.stream()
                .filter(review -> userId.equals(review.getUserId())).findFirst().orElse(null);
        BigDecimal average = reviews.isEmpty() ? courseMapper.selectById(courseId).getRating()
                : BigDecimal.valueOf(reviews.stream().mapToInt(CourseReview::getRating).average().orElse(0))
                .setScale(1, RoundingMode.HALF_UP);
        boolean favorite = userId != null && favoriteMapper.selectCount(new LambdaQueryWrapper<CourseFavorite>()
                .eq(CourseFavorite::getCourseId, courseId).eq(CourseFavorite::getUserId, userId)) > 0;
        return CourseInteractionVO.builder().favorite(favorite).favoriteCount(favoriteCount)
                .reviewCount((long) reviews.size()).averageRating(average)
                .myReview(mine == null ? null : toVO(mine, userId)).build();
    }

    @Transactional
    public void favorite(Long courseId, Long userId) {
        requireUser(userId);
        requireCourse(courseId);
        CourseFavorite favorite = new CourseFavorite();
        favorite.setUserId(userId);
        favorite.setCourseId(courseId);
        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException ignored) {
            // Idempotent: repeated favorite requests keep the desired state.
        }
    }

    @Transactional
    public void unfavorite(Long courseId, Long userId) {
        requireUser(userId);
        favoriteMapper.delete(new LambdaQueryWrapper<CourseFavorite>()
                .eq(CourseFavorite::getCourseId, courseId).eq(CourseFavorite::getUserId, userId));
    }

    public PageResult<CourseVO> listFavorites(Long userId, long page, long size) {
        requireUser(userId);
        long safePage = Math.max(1, page);
        long safeSize = Math.max(1, Math.min(size, 50));
        Page<CourseFavorite> favorites = favoriteMapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<CourseFavorite>().eq(CourseFavorite::getUserId, userId)
                        .orderByDesc(CourseFavorite::getCreatedAt));
        List<CourseVO> courses = favorites.getRecords().stream().map(item -> courseMapper.selectById(item.getCourseId()))
                .filter(java.util.Objects::nonNull).map(this::toCourseVO).toList();
        return PageResult.of(safePage, safeSize, favorites.getTotal(), courses);
    }

    public PageResult<CourseReviewVO> listReviews(Long courseId, Long userId, long page, long size) {
        requireCourse(courseId);
        long safePage = Math.max(1, page);
        long safeSize = Math.max(1, Math.min(size, 50));
        Page<CourseReview> reviews = reviewMapper.selectPage(new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<CourseReview>().eq(CourseReview::getCourseId, courseId)
                        .orderByDesc(CourseReview::getUpdatedAt));
        return PageResult.of(safePage, safeSize, reviews.getTotal(),
                reviews.getRecords().stream().map(review -> toVO(review, userId)).toList());
    }

    @Transactional
    public CourseReviewVO review(Long courseId, Long userId, String username, CourseReviewRequest request) {
        requireUser(userId);
        Course course = requireCourse(courseId);
        CourseReview review = reviewMapper.selectOne(new LambdaQueryWrapper<CourseReview>()
                .eq(CourseReview::getCourseId, courseId).eq(CourseReview::getUserId, userId));
        if (review == null) {
            review = new CourseReview();
            review.setCourseId(courseId);
            review.setUserId(userId);
            review.setUsername(username == null || username.isBlank() ? "用户" + userId : username);
            review.setRating(request.getRating());
            review.setContent(request.getContent().trim());
            reviewMapper.insert(review);
        } else {
            review.setRating(request.getRating());
            review.setContent(request.getContent().trim());
            reviewMapper.updateById(review);
        }
        updateCourseRating(course);
        return toVO(review, userId);
    }

    private void updateCourseRating(Course course) {
        List<CourseReview> reviews = reviewMapper.selectList(new LambdaQueryWrapper<CourseReview>()
                .eq(CourseReview::getCourseId, course.getId()));
        if (!reviews.isEmpty()) {
            BigDecimal average = BigDecimal.valueOf(reviews.stream().mapToInt(CourseReview::getRating).average().orElse(5))
                    .setScale(1, RoundingMode.HALF_UP);
            course.setRating(average);
            courseMapper.updateById(course);
        }
    }

    private Course requireCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getStatus() != 1) throw new BusinessException("课程不存在或未发布");
        return course;
    }

    private void requireUser(Long userId) {
        if (userId == null) throw new BusinessException(401, "请先登录");
    }

    private CourseReviewVO toVO(CourseReview review, Long userId) {
        return CourseReviewVO.builder().id(review.getId()).userId(review.getUserId()).username(review.getUsername())
                .rating(review.getRating()).content(review.getContent()).updatedAt(review.getUpdatedAt())
                .mine(userId != null && userId.equals(review.getUserId())).build();
    }

    private CourseVO toCourseVO(Course course) {
        CourseVO vo = new CourseVO();
        org.springframework.beans.BeanUtils.copyProperties(course, vo);
        return vo;
    }
}
