package com.kjt.lms.service;

import com.kjt.lms.model.request.course.CreateCourseRequestDto;
import com.kjt.lms.model.request.course.UpdateCourseRequestDto;
import com.kjt.lms.model.request.course.SearchCourseRequest;
import com.kjt.lms.model.response.course.CourseCreateResponseDto;
import com.kjt.lms.model.response.course.CourseDetailResponseDto;
import com.kjt.lms.model.response.course.CourseListItemResponseDto;
import com.kjt.lms.model.response.course.CourseUpdateResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface CourseService {

    CourseCreateResponseDto createCourse(CreateCourseRequestDto request);

    CourseUpdateResponseDto updateCourse(UUID courseId, UpdateCourseRequestDto request);

    CourseDetailResponseDto getCourseById(UUID courseId);

    Page<CourseListItemResponseDto> getInstructorCourses(Pageable pageable);

    Page<CourseListItemResponseDto> searchCourses(SearchCourseRequest request, Pageable pageable);

    Page<CourseListItemResponseDto> getCoursesByCategory(UUID categoryId, Pageable pageable);

    Page<CourseListItemResponseDto> getTopRatedCourses(Pageable pageable);

    Page<CourseListItemResponseDto> getTrendingCourses(Pageable pageable);

    CourseUpdateResponseDto publishCourse(UUID courseId);

    CourseUpdateResponseDto unpublishCourse(UUID courseId);

    void deleteCourse(UUID courseId);

    CourseUpdateResponseDto approveCourse(UUID courseId);

    CourseUpdateResponseDto rejectCourse(UUID courseId, String reason);

    CourseUpdateResponseDto uploadCourseImage(UUID courseId, MultipartFile file);

    CourseUpdateResponseDto uploadCoursePreviewVideo(UUID courseId, MultipartFile file);
}
