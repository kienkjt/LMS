package com.kjt.lms.service;

import com.kjt.lms.model.request.lesson.CreateLessonRequestDto;
import com.kjt.lms.model.response.LessonResponseDto;

import java.util.UUID;

public interface LessonService {

    LessonResponseDto createLesson(UUID courseId, UUID chapterId, CreateLessonRequestDto request);
}

