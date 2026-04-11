package com.kjt.lms.controller;

import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.validator.PageableValidator;
import com.kjt.lms.model.request.course.CreateCourseRequestDto;
import com.kjt.lms.model.request.course.RejectCourseRequest;
import com.kjt.lms.model.request.course.SearchCourseRequest;
import com.kjt.lms.model.request.course.UpdateCourseRequestDto;
import com.kjt.lms.model.response.CourseCreateResponseDto;
import com.kjt.lms.model.response.CourseDetailResponseDto;
import com.kjt.lms.model.response.CourseListItemResponseDto;
import com.kjt.lms.model.response.CourseResponseDto;
import com.kjt.lms.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course management endpoints")
public class CourseController {

    private final CourseService courseService;
    private final MessageProvider messageProvider;

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Create a new course", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<CourseCreateResponseDto>> createCourse(
            @Valid @RequestBody CreateCourseRequestDto request) {
        CourseCreateResponseDto response = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.success(response, messageProvider.getMessage("course.created.success")));
    }

    @PutMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Update course", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<CourseResponseDto>> updateCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateCourseRequestDto request) {
        CourseResponseDto response = courseService.updateCourse(courseId, request);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("course.updated.success")));
    }

    @GetMapping("/{courseId}")
    @Operation(summary = "Get course detail by ID")
    public ResponseEntity<APIResponse<CourseDetailResponseDto>> getCourseById(
            @PathVariable UUID courseId) {
        CourseDetailResponseDto response = courseService.getCourseById(courseId);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @GetMapping("/my-courses")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get my courses", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Page<CourseListItemResponseDto>>> getMyInstructorCourses(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        pageable = PageableValidator.validate(pageable);
        Page<CourseListItemResponseDto> response = courseService.getInstructorCourses(pageable);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @GetMapping
    @Operation(summary = "Get all published courses (default search)")
    public ResponseEntity<APIResponse<Page<CourseListItemResponseDto>>> getPublishedCourses(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        pageable = PageableValidator.validate(pageable);
        SearchCourseRequest request = SearchCourseRequest.builder()
                .keyword(keyword)
                .build();
        Page<CourseListItemResponseDto> response = courseService.searchCourses(request, pageable);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @GetMapping("/by-category/{categoryId}")
    @Operation(summary = "Get courses by category (published & active only)")
    public ResponseEntity<APIResponse<Page<CourseListItemResponseDto>>> getCoursesByCategory(
            @PathVariable UUID categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        pageable = PageableValidator.validate(pageable);
        Page<CourseListItemResponseDto> response = courseService.getCoursesByCategory(categoryId, pageable);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }


    @GetMapping("/trending")
    @Operation(summary = "Get trending courses")
    public ResponseEntity<APIResponse<Page<CourseListItemResponseDto>>> getTrendingCourses(
            @PageableDefault(size = 20, sort = "totalStudents", direction = Sort.Direction.DESC) Pageable pageable) {
        pageable = PageableValidator.validate(pageable);
        Page<CourseListItemResponseDto> response = courseService.getTrendingCourses(pageable);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @GetMapping("/top-rated")
    @Operation(summary = "Get top rated courses")
    public ResponseEntity<APIResponse<Page<CourseListItemResponseDto>>> getTopRatedCourses(
            @PageableDefault(size = 20, sort = "avgRating", direction = Sort.Direction.DESC) Pageable pageable) {
        pageable = PageableValidator.validate(pageable);
        Page<CourseListItemResponseDto> response = courseService.getTopRatedCourses(pageable);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @PostMapping("/{courseId}/publish")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Publish course", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<CourseResponseDto>> publishCourse(
            @PathVariable UUID courseId) {
        CourseResponseDto response = courseService.publishCourse(courseId);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("course.published.success")));
    }

    @PostMapping("/{courseId}/unpublish")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Unpublish course", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<CourseResponseDto>> unpublishCourse(
            @PathVariable UUID courseId) {
        CourseResponseDto response = courseService.unpublishCourse(courseId);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("course.unpublished.success")));
    }

    @PostMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Delete course", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Void>> deleteCourse(
            @PathVariable UUID courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.ok(APIResponse.success(null, messageProvider.getMessage("course.deleted.success")));
    }

    @PostMapping("/search")
    @Operation(summary = "Advanced search courses with filters (keyword, status, level, active)")
    public ResponseEntity<APIResponse<Page<CourseListItemResponseDto>>> advancedSearch(
            @RequestBody(required = false) SearchCourseRequest request,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        pageable = PageableValidator.validate(pageable);

        Page<CourseListItemResponseDto> response = courseService.searchCourses(request, pageable);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @PostMapping("/{courseId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve course (Admin only)", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<CourseResponseDto>> approveCourse(
            @PathVariable UUID courseId) {
        CourseResponseDto response = courseService.approveCourse(courseId);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("course.approved.success")));
    }

    @PostMapping("/{courseId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject course with reason (Admin only)", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<CourseResponseDto>> rejectCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody RejectCourseRequest request) {
        CourseResponseDto response = courseService.rejectCourse(courseId, request.getReason());
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("course.rejected.success")));
    }
}
