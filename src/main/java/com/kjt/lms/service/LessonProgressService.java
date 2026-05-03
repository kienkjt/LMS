package com.kjt.lms.service;

import com.kjt.lms.model.response.progress.CourseProgressResponseDto;
import com.kjt.lms.model.response.progress.StudentCourseProgressResponseDto;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface LessonProgressService {

    CourseProgressResponseDto completeLesson(UUID courseId, UUID lessonId);

    CourseProgressResponseDto getCourseProgress(UUID courseId);

    Page<StudentCourseProgressResponseDto> getCourseStudentsProgress(UUID courseId, int page, int pageSize);

    StudentCourseProgressResponseDto getStudentCourseProgress(UUID courseId, UUID studentId);
}

