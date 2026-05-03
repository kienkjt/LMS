package com.kjt.lms.service;

import com.kjt.lms.model.request.quiz.CreateQuestionRequestDto;
import com.kjt.lms.model.request.quiz.CreateQuizRequestDto;
import com.kjt.lms.model.request.quiz.SubmitQuizRequestDto;
import com.kjt.lms.model.request.quiz.UpdateQuestionRequestDto;
import com.kjt.lms.model.request.quiz.UpdateQuizRequestDto;
import com.kjt.lms.model.response.quiz.QuestionResponseDto;
import com.kjt.lms.model.response.quiz.QuizAttemptResponseDto;
import com.kjt.lms.model.response.quiz.QuizResponseDto;

import java.util.List;
import java.util.UUID;

public interface QuizService {

    QuizResponseDto createQuiz(UUID courseId, CreateQuizRequestDto request);

    QuizResponseDto updateQuiz(UUID quizId, UpdateQuizRequestDto request);

    void deleteQuiz(UUID quizId);

    List<QuizResponseDto> getCourseQuizzes(UUID courseId);

    QuizResponseDto getQuiz(UUID quizId);

    QuestionResponseDto addQuestion(UUID quizId, CreateQuestionRequestDto request);

    QuestionResponseDto updateQuestion(UUID questionId, UpdateQuestionRequestDto request);

    void deleteQuestion(UUID questionId);

    QuizAttemptResponseDto submitAttempt(UUID quizId, SubmitQuizRequestDto request);

    List<QuizAttemptResponseDto> getMyAttempts(UUID quizId);
}
