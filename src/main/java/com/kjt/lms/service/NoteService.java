package com.kjt.lms.service;

import com.kjt.lms.model.request.note.CreateNoteRequestDto;
import com.kjt.lms.model.request.note.UpdateNoteRequestDto;
import com.kjt.lms.model.response.note.NoteResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NoteService {

    Page<NoteResponseDto> getCourseNotes(UUID courseId, Pageable pageable);

    Page<NoteResponseDto> getLessonNotes(UUID courseId, UUID lessonId, Pageable pageable);

    NoteResponseDto createNote(UUID courseId, UUID lessonId, CreateNoteRequestDto request);

    NoteResponseDto updateNote(UUID noteId, UpdateNoteRequestDto request);

    void deleteNote(UUID noteId);
}
