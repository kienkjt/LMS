package com.kjt.lms.service;

import com.kjt.lms.model.request.course.CreateCourseRequestDto;
import com.kjt.lms.model.request.course.UpdateCourseRequestDto;
import com.kjt.lms.model.response.CourseResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CourseService {

    /**
     * Create a new course
     */
    CourseResponseDto createCourse(CreateCourseRequestDto request);

    /**
     * Update course
     */
    CourseResponseDto updateCourse(UUID courseId, UpdateCourseRequestDto request);

    /**
     * Get course by ID
     */
    CourseResponseDto getCourseById(UUID courseId);

    /**
     * Get course by slug
     */
    CourseResponseDto getCourseBySlug(String slug);

    /**
     * Get all courses by instructor with pagination
     */
    Page<CourseResponseDto> getInstructorCourses(Pageable pageable);

    /**
     * Get all published courses with pagination
     */
    Page<CourseResponseDto> getPublishedCourses(Pageable pageable);

    /**
     * Search courses by keyword
     */
    Page<CourseResponseDto> searchCourses(String keyword, Pageable pageable);

    /**
     * Get courses by category
     */
    Page<CourseResponseDto> getCoursesByCategory(UUID categoryId, Pageable pageable);

    /**
     * Get top rated courses
     */
    Page<CourseResponseDto> getTopRatedCourses(Pageable pageable);

    /**
     * Get trending courses
     */
    Page<CourseResponseDto> getTrendingCourses(Pageable pageable);

    /**
     * Publish course (change status to PUBLISHED)
     */
    CourseResponseDto publishCourse(UUID courseId);

    /**
     * Unpublish course (change status to DRAFT)
     */
    CourseResponseDto unpublishCourse(UUID courseId);

    /**
     * Delete course (soft delete)
     */
    void deleteCourse(UUID courseId);

    /**
     * Approve course (for admin/moderator)
     */
    CourseResponseDto approveCourse(UUID courseId);

    /**
     * Reject course with reason
     */
    CourseResponseDto rejectCourse(UUID courseId, String reason);
}

