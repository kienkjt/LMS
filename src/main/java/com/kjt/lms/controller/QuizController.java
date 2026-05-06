package com.kjt.lms.controller;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.request.quiz.CreateQuestionRequestDto;
import com.kjt.lms.model.request.quiz.CreateQuizRequestDto;
import com.kjt.lms.model.request.quiz.SubmitQuizRequestDto;
import com.kjt.lms.model.request.quiz.UpdateQuestionRequestDto;
import com.kjt.lms.model.request.quiz.UpdateQuizRequestDto;
import com.kjt.lms.model.response.chapter.ChapterResponseDto;
import com.kjt.lms.model.response.quiz.QuestionResponseDto;
import com.kjt.lms.model.response.quiz.QuizAttemptResponseDto;
import com.kjt.lms.model.response.quiz.QuizResponseDto;
import com.kjt.lms.service.ChapterService;
import com.kjt.lms.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Quizzes", description = "Quiz management and student attempts")
public class QuizController {

    private final QuizService quizService;
    private final ChapterService chapterService;
    private final MessageProvider messageProvider;

    @GetMapping("/courses/{courseId}/quiz-selection")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get course chapters and lessons for quiz creation", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<List<ChapterResponseDto>>> getChaptersForQuizSelection(@PathVariable UUID courseId) {
        return ResponseEntity.ok(APIResponse.success(chapterService.getChaptersByCourse(courseId), null));
    }

    @PostMapping("/courses/{courseId}/quizzes")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Create quiz for a course", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<QuizResponseDto>> createQuiz(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateQuizRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.success(quizService.createQuiz(courseId, request), "Tao quiz thanh cong"));
    }

    @GetMapping("/courses/{courseId}/quizzes")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get course quizzes", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<List<QuizResponseDto>>> getCourseQuizzes(@PathVariable UUID courseId) {
        return ResponseEntity.ok(APIResponse.success(quizService.getCourseQuizzes(courseId), null));
    }

    @GetMapping("/quizzes/{quizId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get quiz detail", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<QuizResponseDto>> getQuiz(@PathVariable UUID quizId) {
        return ResponseEntity.ok(APIResponse.success(quizService.getQuiz(quizId), null));
    }

    @PutMapping("/quizzes/{quizId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Update quiz", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<QuizResponseDto>> updateQuiz(
            @PathVariable UUID quizId,
            @Valid @RequestBody UpdateQuizRequestDto request) {
        return ResponseEntity.ok(APIResponse.success(quizService.updateQuiz(quizId, request), "Cap nhat quiz thanh cong"));
    }

    @DeleteMapping("/quizzes/{quizId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Delete quiz", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Void>> deleteQuiz(@PathVariable UUID quizId) {
        quizService.deleteQuiz(quizId);
        return ResponseEntity.ok(APIResponse.success(null, "Xoa quiz thanh cong"));
    }

    @PostMapping("/quizzes/{quizId}/questions")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Add question to quiz", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<QuestionResponseDto>> addQuestion(
            @PathVariable UUID quizId,
            @Valid @RequestBody CreateQuestionRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.success(quizService.addQuestion(quizId, request), "Tao cau hoi thanh cong"));
    }

    @PutMapping("/quizzes/questions/{questionId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Update question", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<QuestionResponseDto>> updateQuestion(
            @PathVariable UUID questionId,
            @Valid @RequestBody UpdateQuestionRequestDto request) {
        return ResponseEntity.ok(APIResponse.success(quizService.updateQuestion(questionId, request), "Cap nhat cau hoi thanh cong"));
    }

    @DeleteMapping("/quizzes/questions/{questionId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Delete question", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Void>> deleteQuestion(@PathVariable UUID questionId) {
        quizService.deleteQuestion(questionId);
        return ResponseEntity.ok(APIResponse.success(null, "Xoa cau hoi thanh cong"));
    }

    @PostMapping("/quizzes/{quizId}/attempts")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Submit quiz attempt", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<QuizAttemptResponseDto>> submitAttempt(
            @PathVariable UUID quizId,
            @Valid @RequestBody SubmitQuizRequestDto request) {
        return ResponseEntity.ok(APIResponse.success(quizService.submitAttempt(quizId, request), "Nop bai quiz thanh cong"));
    }

    @GetMapping("/quizzes/{quizId}/attempts/my")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Get my quiz attempts", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<List<QuizAttemptResponseDto>>> getMyAttempts(@PathVariable UUID quizId) {
        return ResponseEntity.ok(APIResponse.success(quizService.getMyAttempts(quizId), null));
    }
}
