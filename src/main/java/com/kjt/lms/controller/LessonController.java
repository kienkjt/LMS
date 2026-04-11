package com.kjt.lms.controller;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.request.lesson.CreateLessonRequestDto;
import com.kjt.lms.model.request.lesson.UpdateLessonRequestDto;
import com.kjt.lms.model.response.LessonResponseDto;
import com.kjt.lms.service.LessonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/chapters/{chapterId}/lessons")
@RequiredArgsConstructor
@Tag(name = "Lessons", description = "Lesson management endpoints")
public class LessonController {

    private final LessonService lessonService;
    private final MessageProvider messageProvider;

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Create lesson for a chapter", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<LessonResponseDto>> createLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId,
            @Valid @RequestBody CreateLessonRequestDto request) {
        LessonResponseDto response = lessonService.createLesson(courseId, chapterId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.success(response, messageProvider.getMessage("lesson.created.success")));
    }

    @GetMapping
    @Operation(summary = "Get all lessons by chapter")
    public ResponseEntity<APIResponse<List<LessonResponseDto>>> getLessonsByChapter(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId) {
        List<LessonResponseDto> response = lessonService.getLessonsByChapter(courseId, chapterId);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @GetMapping("/{lessonId}")
    @Operation(summary = "Get lesson by ID")
    public ResponseEntity<APIResponse<LessonResponseDto>> getLessonById(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId,
            @PathVariable UUID lessonId) {
        LessonResponseDto response = lessonService.getLessonById(courseId, chapterId, lessonId);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @PutMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Update lesson", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<LessonResponseDto>> updateLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId,
            @PathVariable UUID lessonId,
            @Valid @RequestBody UpdateLessonRequestDto request) {
        LessonResponseDto response = lessonService.updateLesson(courseId, chapterId, lessonId, request);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("lesson.updated.success")));
    }

    @PostMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Delete lesson", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Void>> deleteLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId,
            @PathVariable UUID lessonId) {
        lessonService.deleteLesson(courseId, chapterId, lessonId);
        return ResponseEntity.ok(APIResponse.success(null, messageProvider.getMessage("lesson.deleted.success")));
    }
}
