package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.LessonEntity;
import com.kjt.lms.model.entity.NoteEntity;
import com.kjt.lms.model.request.note.CreateNoteRequestDto;
import com.kjt.lms.model.request.note.UpdateNoteRequestDto;
import com.kjt.lms.model.response.note.NoteResponseDto;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.LessonRepository;
import com.kjt.lms.repository.NoteRepository;
import com.kjt.lms.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteServiceImpl extends BaseService implements NoteService {

    private final NoteRepository noteRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MessageProvider messageProvider;

    @Override
    @Transactional(readOnly = true)
    public Page<NoteResponseDto> getCourseNotes(UUID courseId, Pageable pageable) {
        UUID studentId = securityUtils.getCurrentUserId();
        validateStudentEnrolled(studentId, courseId);
        return noteRepository.findByStudentIdAndCourseIdAndDeletedFalseOrderByCreatedAtDesc(studentId, courseId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NoteResponseDto> getLessonNotes(UUID courseId, UUID lessonId, Pageable pageable) {
        UUID studentId = securityUtils.getCurrentUserId();
        validateLessonInCourse(courseId, lessonId);
        validateStudentEnrolled(studentId, courseId);
        return noteRepository.findByStudentIdAndCourseIdAndLessonIdAndDeletedFalseOrderByVideoTimestampAscCreatedAtAsc(
                studentId,
                courseId,
                lessonId,
                pageable
        ).map(this::toResponse);
    }

    @Override
    @Transactional
    public NoteResponseDto createNote(UUID courseId, UUID lessonId, CreateNoteRequestDto request) {
        UUID studentId = securityUtils.getCurrentUserId();
        validateLessonInCourse(courseId, lessonId);
        validateStudentEnrolled(studentId, courseId);

        NoteEntity note = NoteEntity.builder()
                .studentId(studentId)
                .courseId(courseId)
                .lessonId(lessonId)
                .content(request.getContent())
                .videoTimestamp(request.getVideoTimestamp())
                .build();

        NoteEntity savedNote = noteRepository.save(note);
        log.info("Student {} created note {} for lesson {}", studentId, savedNote.getId(), lessonId);
        return toResponse(savedNote);
    }

    @Override
    @Transactional
    public NoteResponseDto updateNote(UUID noteId, UpdateNoteRequestDto request) {
        UUID studentId = securityUtils.getCurrentUserId();
        NoteEntity note = noteRepository.findByIdAndStudentIdAndDeletedFalse(noteId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.note.notFound")));

        validateLessonInCourse(note.getCourseId(), note.getLessonId());
        validateStudentEnrolled(studentId, note.getCourseId());

        note.setContent(request.getContent());
        note.setVideoTimestamp(request.getVideoTimestamp());
        return toResponse(noteRepository.save(note));
    }

    @Override
    @Transactional
    public void deleteNote(UUID noteId) {
        UUID studentId = securityUtils.getCurrentUserId();
        NoteEntity note = noteRepository.findByIdAndStudentIdAndDeletedFalse(noteId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.note.notFound")));

        note.setDeleted(true);
        noteRepository.save(note);
    }

    private void validateStudentEnrolled(UUID studentId, UUID courseId) {
        findActiveCourseById(courseId);
        if (!enrollmentRepository.existsByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId)) {
            throw new BusinessException(messageProvider.getMessage("exception.enrollment.required"));
        }
    }

    private LessonEntity validateLessonInCourse(UUID courseId, UUID lessonId) {
        LessonEntity lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.lesson.notFound")));
        if (!lesson.getCourseId().equals(courseId)) {
            throw new BusinessException(messageProvider.getMessage("exception.lesson.notBelongToCourse"));
        }
        return lesson;
    }

    private NoteResponseDto toResponse(NoteEntity note) {
        LessonEntity lesson = lessonRepository.findByIdAndDeletedFalse(note.getLessonId()).orElse(null);
        return NoteResponseDto.builder()
                .id(note.getId())
                .studentId(note.getStudentId())
                .courseId(note.getCourseId())
                .lessonId(note.getLessonId())
                .lessonTitle(lesson == null ? null : lesson.getTitle())
                .content(note.getContent())
                .videoTimestamp(note.getVideoTimestamp())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
