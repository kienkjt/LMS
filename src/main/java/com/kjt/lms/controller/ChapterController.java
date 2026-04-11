package com.kjt.lms.controller;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.request.chapter.CreateChapterRequestDto;
import com.kjt.lms.model.request.chapter.UpdateChapterRequestDto;
import com.kjt.lms.model.response.ChapterResponseDto;
import com.kjt.lms.service.ChapterService;
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
@RequestMapping("/api/v1/courses/{courseId}/chapters")
@RequiredArgsConstructor
@Tag(name = "Chapters", description = "Chapter management endpoints")
public class ChapterController {

    private final ChapterService chapterService;
    private final MessageProvider messageProvider;

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Create chapter for a course", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<ChapterResponseDto>> createChapter(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateChapterRequestDto request) {
        ChapterResponseDto response = chapterService.createChapter(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.success(response, messageProvider.getMessage("chapter.created.success")));
    }

    @GetMapping
    @Operation(summary = "Get all chapters by course")
    public ResponseEntity<APIResponse<List<ChapterResponseDto>>> getChaptersByCourse(
            @PathVariable UUID courseId) {
        List<ChapterResponseDto> response = chapterService.getChaptersByCourse(courseId);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @GetMapping("/{chapterId}")
    @Operation(summary = "Get chapter by ID")
    public ResponseEntity<APIResponse<ChapterResponseDto>> getChapterById(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId) {
        ChapterResponseDto response = chapterService.getChapterById(courseId, chapterId);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @PutMapping("/{chapterId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Update chapter", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<ChapterResponseDto>> updateChapter(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId,
            @Valid @RequestBody UpdateChapterRequestDto request) {
        ChapterResponseDto response = chapterService.updateChapter(courseId, chapterId, request);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("chapter.updated.success")));
    }

    @PostMapping("/{chapterId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Delete chapter", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Void>> deleteChapter(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId) {
        chapterService.deleteChapter(courseId, chapterId);
        return ResponseEntity.ok(APIResponse.success(null, messageProvider.getMessage("chapter.deleted.success")));
    }
}
