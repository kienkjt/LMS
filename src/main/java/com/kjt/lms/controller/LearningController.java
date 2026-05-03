package com.kjt.lms.controller;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.response.enrollment.EnrolledCourseResponseDto;
import com.kjt.lms.model.response.enrollment.EnrollmentResponseDto;
import com.kjt.lms.model.response.progress.CourseProgressResponseDto;
import com.kjt.lms.model.response.progress.StudentCourseProgressResponseDto;
import com.kjt.lms.service.EnrollmentService;
import com.kjt.lms.service.LessonProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
@Tag(name = "Learning", description = "Enrollment and learning progress endpoints")
public class LearningController {

    private final EnrollmentService enrollmentService;
    private final LessonProgressService lessonProgressService;
    private final MessageProvider messageProvider;

    @PostMapping("/courses/{courseId}/enroll")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Enroll current user to a course", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<EnrollmentResponseDto>> enrollCourse(@PathVariable UUID courseId) {
        EnrollmentResponseDto response = enrollmentService.enrollCourse(courseId);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("enrollment.created.success")));
    }

    @PostMapping("/courses/{courseId}/lessons/{lessonId}/complete")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Mark lesson as completed", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<CourseProgressResponseDto>> completeLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId) {
        CourseProgressResponseDto response = lessonProgressService.completeLesson(courseId, lessonId);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("lesson.progress.updated.success")));
    }

    @GetMapping("/courses/{courseId}/progress")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get learning progress for a course", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<CourseProgressResponseDto>> getCourseProgress(@PathVariable UUID courseId) {
        CourseProgressResponseDto response = lessonProgressService.getCourseProgress(courseId);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @GetMapping("/my-courses")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get all enrolled courses of current user", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<List<EnrolledCourseResponseDto>>> getMyEnrolledCourses() {
        List<EnrolledCourseResponseDto> response = enrollmentService.getMyEnrolledCourses();
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("enrollment.list.success")));
    }

    @GetMapping("/instructor/courses/{courseId}/students")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get enrolled students progress for an instructor course", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Page<StudentCourseProgressResponseDto>>> getCourseStudentsForInstructor(
            @PathVariable UUID courseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Page<StudentCourseProgressResponseDto> response =
                lessonProgressService.getCourseStudentsProgress(courseId, page, pageSize);

        return ResponseEntity.ok(APIResponse.success(
                response,
                messageProvider.getMessage("lesson.progress.list.success")
        ));
    }

    @GetMapping("/instructor/courses/{courseId}/students/{studentId}/progress")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Track a student's detailed progress in an instructor course", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<StudentCourseProgressResponseDto>> getStudentCourseProgressForInstructor(
            @PathVariable UUID courseId,
            @PathVariable UUID studentId) {
        StudentCourseProgressResponseDto response = lessonProgressService.getStudentCourseProgress(courseId, studentId);
        return ResponseEntity.ok(APIResponse.success(
                response,
                messageProvider.getMessage("lesson.progress.detail.success")
        ));
    }
}

