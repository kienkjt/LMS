package com.kjt.lms.service;

import com.kjt.lms.model.response.progress.CourseProgressResponseDto;

import java.util.UUID;

public interface LessonProgressService {

    CourseProgressResponseDto completeLesson(UUID courseId, UUID lessonId);

    CourseProgressResponseDto getCourseProgress(UUID courseId);
}

