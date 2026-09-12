package com.edu.course.service;

import com.edu.common.core.exception.BusinessException;
import com.edu.course.dto.CourseReviewRequest;
import com.edu.course.entity.*;
import com.edu.course.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CourseInteractionServiceTest {
    private CourseMapper courseMapper;
    private CourseFavoriteMapper favoriteMapper;
    private CourseReviewMapper reviewMapper;
    private CourseInteractionService service;

    @BeforeEach
    void setUp() {
        courseMapper = mock(CourseMapper.class);
        favoriteMapper = mock(CourseFavoriteMapper.class);
        reviewMapper = mock(CourseReviewMapper.class);
        service = new CourseInteractionService(courseMapper, favoriteMapper, reviewMapper);
    }

    @Test
    void favoriteRequiresAuthenticatedUser() {
        BusinessException error = assertThrows(BusinessException.class, () -> service.favorite(1L, null));
        assertEquals(401, error.getCode());
        verifyNoInteractions(courseMapper, favoriteMapper);
    }

    @Test
    void repeatedFavoriteIsIdempotent() {
        when(courseMapper.selectById(1L)).thenReturn(publishedCourse());
        when(favoriteMapper.insert(any(CourseFavorite.class))).thenThrow(new DuplicateKeyException("duplicate"));
        assertDoesNotThrow(() -> service.favorite(1L, 7L));
    }

    @Test
    void reviewUpdatesCourseAggregateRating() {
        Course course = publishedCourse();
        when(courseMapper.selectById(1L)).thenReturn(course);
        when(reviewMapper.selectOne(any())).thenReturn(null);
        when(reviewMapper.insert(any(CourseReview.class))).thenAnswer(invocation -> {
            CourseReview inserted = invocation.getArgument(0);
            inserted.setId(99L);
            return 1;
        });
        CourseReview previous = new CourseReview();
        previous.setRating(3);
        CourseReview current = new CourseReview();
        current.setRating(5);
        when(reviewMapper.selectList(any())).thenReturn(List.of(previous, current));

        CourseReviewRequest request = new CourseReviewRequest();
        request.setRating(5);
        request.setContent("内容扎实，项目案例很有帮助");
        service.review(1L, 7L, "student", request);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseMapper).updateById(captor.capture());
        assertEquals(new BigDecimal("4.0"), captor.getValue().getRating());
    }

    private Course publishedCourse() {
        Course course = new Course();
        course.setId(1L);
        course.setStatus(1);
        course.setRating(new BigDecimal("5.0"));
        return course;
    }
}
