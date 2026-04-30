package com.kjt.lms.controller;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.common.validator.Common;
import com.kjt.lms.model.request.note.CreateNoteRequestDto;
import com.kjt.lms.model.request.note.UpdateNoteRequestDto;
import com.kjt.lms.model.response.note.NoteResponseDto;
import com.kjt.lms.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
@Tag(name = "Notes", description = "Student lesson notes")
@PreAuthorize("hasRole('STUDENT')")
public class NoteController {

    private final NoteService noteService;
    private final MessageProvider messageProvider;

    @GetMapping("/courses/{courseId}/notes")
    @Operation(summary = "Get current student's notes for a course", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Page<NoteResponseDto>>> getCourseNotes(
            @PathVariable UUID courseId,
            @RequestParam(value = "page", defaultValue = Common.PAGE_DEFAULT) Integer page,
            @RequestParam(value = "pageSize", defaultValue = Common.PAGE_SIZE_DEFAULT) Integer pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<NoteResponseDto> response = noteService.getCourseNotes(courseId, pageable);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @GetMapping("/courses/{courseId}/lessons/{lessonId}/notes")
    @Operation(summary = "Get current student's notes for a lesson", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Page<NoteResponseDto>>> getLessonNotes(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId,
            @RequestParam(value = "page", defaultValue = Common.PAGE_DEFAULT) Integer page,
            @RequestParam(value = "pageSize", defaultValue = Common.PAGE_SIZE_DEFAULT) Integer pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<NoteResponseDto> response = noteService.getLessonNotes(courseId, lessonId, pageable);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @PostMapping("/courses/{courseId}/lessons/{lessonId}/notes")
    @Operation(summary = "Create a lesson note", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<NoteResponseDto>> createNote(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId,
            @Valid @RequestBody CreateNoteRequestDto request) {
        NoteResponseDto response = noteService.createNote(courseId, lessonId, request);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("note.created.success")));
    }

    @PutMapping("/notes/{noteId}")
    @Operation(summary = "Update a note", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<NoteResponseDto>> updateNote(
            @PathVariable UUID noteId,
            @Valid @RequestBody UpdateNoteRequestDto request) {
        NoteResponseDto response = noteService.updateNote(noteId, request);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("note.updated.success")));
    }

    @DeleteMapping("/notes/{noteId}")
    @Operation(summary = "Delete a note", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Void>> deleteNote(@PathVariable UUID noteId) {
        noteService.deleteNote(noteId);
        return ResponseEntity.ok(APIResponse.success(null, messageProvider.getMessage("note.deleted.success")));
    }
}
