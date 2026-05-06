package com.kjt.lms.model.response.chapter;

import com.kjt.lms.model.response.lesson.LessonResponseDto;
import com.kjt.lms.model.response.quiz.QuizResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterResponseDto {

    private UUID id;
    private UUID courseId;
    private String title;
    private String description;
    private Integer totalLessons;
    private Integer totalDuration;
    private List<LessonResponseDto> lessons;
    private List<QuizResponseDto> quizzes;
}

