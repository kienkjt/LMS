package com.kjt.lms.service;

import com.kjt.lms.model.request.lesson.CreateLessonRequestDto;
import com.kjt.lms.model.request.lesson.UpdateLessonRequestDto;
import com.kjt.lms.model.response.lesson.LessonResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface LessonService {

    LessonResponseDto createLesson(UUID courseId, UUID chapterId, CreateLessonRequestDto request);

    LessonResponseDto getLessonById(UUID courseId, UUID chapterId, UUID lessonId);

    List<LessonResponseDto> getLessonsByChapter(UUID courseId, UUID chapterId);

    LessonResponseDto updateLesson(UUID courseId, UUID chapterId, UUID lessonId, UpdateLessonRequestDto request);

    void deleteLesson(UUID courseId, UUID chapterId, UUID lessonId);

    LessonResponseDto uploadLessonVideo(UUID courseId, UUID chapterId, UUID lessonId, MultipartFile file);
}
