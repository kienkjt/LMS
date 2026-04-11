package com.kjt.lms.service;

import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.constants.CourseStatusEnum;
import com.kjt.lms.model.request.course.CreateCourseRequestDto;
import com.kjt.lms.model.request.course.UpdateCourseRequestDto;
import com.kjt.lms.model.request.course.SearchCourseRequest;
import com.kjt.lms.model.response.CourseCreateResponseDto;
import com.kjt.lms.model.response.CourseDetailResponseDto;
import com.kjt.lms.model.response.CourseListItemResponseDto;
import com.kjt.lms.model.response.CourseResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CourseService {

    /**
     * Create a new course
     */
    CourseCreateResponseDto createCourse(CreateCourseRequestDto request);

    /**
     * Update course
     */
    CourseResponseDto updateCourse(UUID courseId, UpdateCourseRequestDto request);

    /**
     * Get course detail by ID
     */
    CourseDetailResponseDto getCourseById(UUID courseId);

    /**
     * Get all courses by instructor with pagination
     */
    Page<CourseListItemResponseDto> getInstructorCourses(Pageable pageable);

    /**
     * Search and filter courses (keyword, status, level, active)
     * SearchCourseRequest defaults to PUBLISHED + ACTIVE status
     */
    Page<CourseListItemResponseDto> searchCourses(SearchCourseRequest request, Pageable pageable);

    /**
     * Get courses by category
     */
    Page<CourseListItemResponseDto> getCoursesByCategory(UUID categoryId, Pageable pageable);

    /**
     * Get top rated courses
     */
    Page<CourseListItemResponseDto> getTopRatedCourses(Pageable pageable);

    /**
     * Get trending courses
     */
    Page<CourseListItemResponseDto> getTrendingCourses(Pageable pageable);

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

