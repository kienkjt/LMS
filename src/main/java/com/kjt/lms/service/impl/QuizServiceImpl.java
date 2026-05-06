package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.QuizTypeEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.ChapterEntity;
import com.kjt.lms.model.entity.LessonEntity;
import com.kjt.lms.model.entity.QuestionEntity;
import com.kjt.lms.model.entity.QuizAnswerEntity;
import com.kjt.lms.model.entity.QuizAttemptEntity;
import com.kjt.lms.model.entity.QuizEntity;
import com.kjt.lms.model.request.quiz.CreateQuestionRequestDto;
import com.kjt.lms.model.request.quiz.CreateQuizRequestDto;
import com.kjt.lms.model.request.quiz.SubmitQuizRequestDto;
import com.kjt.lms.model.request.quiz.UpdateQuestionRequestDto;
import com.kjt.lms.model.request.quiz.UpdateQuizRequestDto;
import com.kjt.lms.model.response.quiz.QuestionResponseDto;
import com.kjt.lms.model.response.quiz.QuizAnswerResultResponseDto;
import com.kjt.lms.model.response.quiz.QuizAttemptResponseDto;
import com.kjt.lms.model.response.quiz.QuizResponseDto;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.ChapterRepository;
import com.kjt.lms.repository.LessonRepository;
import com.kjt.lms.repository.QuestionRepository;
import com.kjt.lms.repository.QuizAnswerRepository;
import com.kjt.lms.repository.QuizAttemptRepository;
import com.kjt.lms.repository.QuizRepository;
import com.kjt.lms.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizServiceImpl extends BaseService implements QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MessageProvider messageProvider;

    @Override
    @Transactional
    public QuizResponseDto createQuiz(UUID courseId, CreateQuizRequestDto request) {
        CourseEntity course = findActiveCourseById(courseId);
        validateCourseOwnership(course);
        if (request.getChapterId() == null && request.getLessonId() == null) {
            throw new BusinessException("Vui long chon chapter hoac bai hoc cho quiz");
        }
        validateChapterBelongsToCourse(request.getChapterId(), courseId);
        LessonEntity lesson = validateLessonBelongsToCourse(request.getLessonId(), courseId);
        UUID normalizedChapterId = resolveChapterId(request.getChapterId(), lesson);
        validateLessonChapterConsistency(lesson, normalizedChapterId);

        QuizEntity quiz = QuizEntity.builder()
                .courseId(courseId)
                .chapterId(normalizedChapterId)
                .lessonId(request.getLessonId())
                .title(request.getTitle())
                .description(request.getDescription())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .passScore(request.getPassScore())
                .maxAttempts(request.getMaxAttempts())
                .shuffleQuestions(Boolean.TRUE.equals(request.getShuffleQuestions()))
                .build();

        QuizEntity savedQuiz = quizRepository.save(quiz);
        attachQuizToLesson(request.getLessonId(), savedQuiz.getId());
        return toQuizResponse(savedQuiz, true);
    }

    @Override
    @Transactional
    public QuizResponseDto updateQuiz(UUID quizId, UpdateQuizRequestDto request) {
        QuizEntity quiz = getOwnedQuiz(quizId);
        quiz.setTitle(request.getTitle());
        quiz.setDescription(request.getDescription());
        quiz.setTimeLimitMinutes(request.getTimeLimitMinutes());
        quiz.setPassScore(request.getPassScore());
        quiz.setMaxAttempts(request.getMaxAttempts());
        quiz.setShuffleQuestions(Boolean.TRUE.equals(request.getShuffleQuestions()));
        return toQuizResponse(quizRepository.save(quiz), true);
    }

    @Override
    @Transactional
    public void deleteQuiz(UUID quizId) {
        QuizEntity quiz = getOwnedQuiz(quizId);
        quiz.setDeleted(true);
        quizRepository.save(quiz);

        if (quiz.getLessonId() != null) {
            lessonRepository.findByIdAndDeletedFalse(quiz.getLessonId())
                    .ifPresent(lesson -> {
                        if (quiz.getId().equals(lesson.getQuizId())) {
                            lesson.setQuizId(null);
                            lessonRepository.save(lesson);
                        }
                    });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizResponseDto> getCourseQuizzes(UUID courseId) {
        CourseEntity course = findActiveCourseById(courseId);
        validateCanViewCourseQuiz(course);
        return quizRepository.findByCourseIdAndDeletedFalseOrderByCreatedAtAsc(courseId)
                .stream()
                .map(quiz -> toQuizResponse(quiz, securityUtils.isAdmin() || course.getInstructorId().equals(getCurrentUserId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QuizResponseDto getQuiz(UUID quizId) {
        QuizEntity quiz = findQuiz(quizId);
        CourseEntity course = findActiveCourseById(quiz.getCourseId());
        validateCanViewCourseQuiz(course);
        boolean includeAnswers = securityUtils.isAdmin() || course.getInstructorId().equals(getCurrentUserId());
        return toQuizResponse(quiz, includeAnswers);
    }

    @Override
    @Transactional
    public QuestionResponseDto addQuestion(UUID quizId, CreateQuestionRequestDto request) {
        QuizEntity quiz = getOwnedQuiz(quizId);
        QuestionEntity question = QuestionEntity.builder()
                .quizId(quiz.getId())
                .questionText(request.getQuestionText())
                .type(request.getType())
                .options(request.getOptions())
                .correctAnswer(request.getCorrectAnswer())
                .explanation(request.getExplanation())
                .points(request.getPoints())
                .build();
        return toQuestionResponse(questionRepository.save(question), true);
    }

    @Override
    @Transactional
    public QuestionResponseDto updateQuestion(UUID questionId, UpdateQuestionRequestDto request) {
        QuestionEntity question = findQuestion(questionId);
        getOwnedQuiz(question.getQuizId());
        question.setQuestionText(request.getQuestionText());
        question.setType(request.getType());
        question.setOptions(request.getOptions());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setExplanation(request.getExplanation());
        question.setPoints(request.getPoints());
        return toQuestionResponse(questionRepository.save(question), true);
    }

    @Override
    @Transactional
    public void deleteQuestion(UUID questionId) {
        QuestionEntity question = findQuestion(questionId);
        getOwnedQuiz(question.getQuizId());
        question.setDeleted(true);
        questionRepository.save(question);
    }

    @Override
    @Transactional
    public QuizAttemptResponseDto submitAttempt(UUID quizId, SubmitQuizRequestDto request) {
        UUID studentId = securityUtils.getCurrentUserId();
        QuizEntity quiz = findQuiz(quizId);
        validateStudentCanTakeQuiz(studentId, quiz);

        List<QuestionEntity> questions = questionRepository.findByQuizIdAndDeletedFalseOrderByCreatedAtAsc(quizId);
        if (questions.isEmpty()) {
            throw new BusinessException(messageProvider.getMessage("exception.quiz.noQuestions"));
        }

        long previousAttempts = quizAttemptRepository.countByStudentIdAndQuizIdAndDeletedFalse(studentId, quizId);
        if (quiz.getMaxAttempts() != null && quiz.getMaxAttempts() > 0 && previousAttempts >= quiz.getMaxAttempts()) {
            throw new BusinessException(messageProvider.getMessage("exception.quiz.maxAttempts"));
        }

        Map<UUID, QuestionEntity> questionById = questions.stream()
                .collect(Collectors.toMap(QuestionEntity::getId, Function.identity()));
        Map<UUID, String> submittedAnswerByQuestionId = request.getAnswers().stream()
                .collect(Collectors.toMap(
                        SubmitQuizRequestDto.AnswerSubmissionDto::getQuestionId,
                        SubmitQuizRequestDto.AnswerSubmissionDto::getSelectedAnswer,
                        (first, second) -> second
                ));

        int totalPoints = questions.stream().mapToInt(QuestionEntity::getPoints).sum();
        int earnedPoints = 0;
        QuizAttemptEntity attempt = QuizAttemptEntity.builder()
                .studentId(studentId)
                .quizId(quizId)
                .attemptNumber(Math.toIntExact(previousAttempts + 1))
                .totalPoints(totalPoints)
                .startedAt(LocalDateTime.now())
                .submittedAt(LocalDateTime.now())
                .timeSpent(request.getTimeSpent())
                .build();
        attempt = quizAttemptRepository.save(attempt);

        for (Map.Entry<UUID, String> submitted : submittedAnswerByQuestionId.entrySet()) {
            if (!questionById.containsKey(submitted.getKey())) {
                throw new BusinessException(messageProvider.getMessage("exception.quiz.question.notInQuiz"));
            }
        }

        for (QuestionEntity question : questions) {
            String selectedAnswer = submittedAnswerByQuestionId.get(question.getId());
            boolean correct = isCorrect(question, selectedAnswer);
            int questionEarnedPoints = correct ? question.getPoints() : 0;
            earnedPoints += questionEarnedPoints;

            quizAnswerRepository.save(QuizAnswerEntity.builder()
                    .attemptId(attempt.getId())
                    .questionId(question.getId())
                    .selectedAnswer(selectedAnswer)
                    .isCorrect(correct)
                    .earnedPoints(questionEarnedPoints)
                    .build());
        }

        BigDecimal score = BigDecimal.valueOf(earnedPoints)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(totalPoints), 2, RoundingMode.HALF_UP);
        attempt.setEarnedPoints(earnedPoints);
        attempt.setScore(score);
        attempt.setPassed(score.compareTo(quiz.getPassScore()) >= 0);
        attempt = quizAttemptRepository.save(attempt);

        log.info("Student {} submitted quiz {} attempt {} with score {}", studentId, quizId, attempt.getAttemptNumber(), score);
        return toAttemptResponse(attempt, questions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizAttemptResponseDto> getMyAttempts(UUID quizId) {
        UUID studentId = securityUtils.getCurrentUserId();
        QuizEntity quiz = findQuiz(quizId);
        validateStudentCanTakeQuiz(studentId, quiz);
        List<QuestionEntity> questions = questionRepository.findByQuizIdAndDeletedFalseOrderByCreatedAtAsc(quizId);
        return quizAttemptRepository.findByStudentIdAndQuizIdAndDeletedFalseOrderByAttemptNumberDesc(studentId, quizId)
                .stream()
                .map(attempt -> toAttemptResponse(attempt, questions))
                .toList();
    }

    private QuizEntity getOwnedQuiz(UUID quizId) {
        QuizEntity quiz = findQuiz(quizId);
        CourseEntity course = findActiveCourseById(quiz.getCourseId());
        validateCourseOwnership(course);
        return quiz;
    }

    private QuizEntity findQuiz(UUID quizId) {
        return quizRepository.findByIdAndDeletedFalse(quizId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.quiz.notFound")));
    }

    private QuestionEntity findQuestion(UUID questionId) {
        return questionRepository.findByIdAndDeletedFalse(questionId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.quiz.question.notFound")));
    }

    private LessonEntity validateLessonBelongsToCourse(UUID lessonId, UUID courseId) {
        if (lessonId == null) {
            return null;
        }
        LessonEntity lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.lesson.notFound")));
        if (!courseId.equals(lesson.getCourseId())) {
            throw new BusinessException(messageProvider.getMessage("exception.lesson.notBelongToCourse"));
        }
        return lesson;
    }

    private UUID resolveChapterId(UUID chapterId, LessonEntity lesson) {
        if (chapterId != null) {
            return chapterId;
        }
        return lesson != null ? lesson.getChapterId() : null;
    }

    private void validateLessonChapterConsistency(LessonEntity lesson, UUID chapterId) {
        if (lesson == null || chapterId == null) {
            return;
        }
        if (!chapterId.equals(lesson.getChapterId())) {
            throw new BusinessException("Bai hoc khong thuoc chapter da chon");
        }
    }

    private void validateChapterBelongsToCourse(UUID chapterId, UUID courseId) {
        if (chapterId == null) {
            return;
        }
        ChapterEntity chapter = chapterRepository.findByIdAndDeletedFalse(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.chapter.notFound")));
        if (!courseId.equals(chapter.getCourseId())) {
            throw new BusinessException(messageProvider.getMessage("exception.chapter.notBelongToCourse"));
        }
    }

    private void attachQuizToLesson(UUID lessonId, UUID quizId) {
        if (lessonId == null) {
            return;
        }
        lessonRepository.findByIdAndDeletedFalse(lessonId)
                .ifPresent(lesson -> {
                    lesson.setQuizId(quizId);
                    lessonRepository.save(lesson);
                });
    }

    private void validateCanViewCourseQuiz(CourseEntity course) {
        UUID currentUserId = getCurrentUserId();
        if (securityUtils.isAdmin() || course.getInstructorId().equals(currentUserId)) {
            return;
        }
        if (!enrollmentRepository.existsByStudentIdAndCourseIdAndDeletedFalse(currentUserId, course.getId())) {
            throw new BusinessException(messageProvider.getMessage("exception.enrollment.required"));
        }
    }

    private void validateStudentCanTakeQuiz(UUID studentId, QuizEntity quiz) {
        CourseEntity course = findActiveCourseById(quiz.getCourseId());
        if (course.getInstructorId().equals(studentId)) {
            throw new BusinessException(messageProvider.getMessage("exception.quiz.instructorCannotAttempt"));
        }
        if (!securityUtils.isAdmin()
                && !enrollmentRepository.existsByStudentIdAndCourseIdAndDeletedFalse(studentId, quiz.getCourseId())) {
            throw new BusinessException(messageProvider.getMessage("exception.enrollment.required"));
        }
    }

    private UUID getCurrentUserId() {
        return securityUtils.getCurrentUserId();
    }

    private boolean isCorrect(QuestionEntity question, String selectedAnswer) {
        if (selectedAnswer == null) {
            return false;
        }
        if (question.getType() == QuizTypeEnum.MULTIPLE_CHOICE) {
            return normalizeMultiAnswer(question.getCorrectAnswer()).equals(normalizeMultiAnswer(selectedAnswer));
        }
        return normalizeAnswer(question.getCorrectAnswer()).equals(normalizeAnswer(selectedAnswer));
    }

    private String normalizeAnswer(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase();
    }

    private List<String> normalizeMultiAnswer(String answer) {
        return Arrays.stream((answer == null ? "" : answer).split(","))
                .map(this::normalizeAnswer)
                .filter(value -> !value.isBlank())
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private QuizResponseDto toQuizResponse(QuizEntity quiz, boolean includeAnswers) {
        List<QuestionResponseDto> questions = questionRepository.findByQuizIdAndDeletedFalseOrderByCreatedAtAsc(quiz.getId())
                .stream()
                .map(question -> toQuestionResponse(question, includeAnswers))
                .toList();
        return QuizResponseDto.builder()
                .id(quiz.getId())
                .courseId(quiz.getCourseId())
                .chapterId(quiz.getChapterId())
                .lessonId(quiz.getLessonId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .passScore(quiz.getPassScore())
                .maxAttempts(quiz.getMaxAttempts())
                .shuffleQuestions(quiz.getShuffleQuestions())
                .totalQuestions((long) questions.size())
                .createdAt(quiz.getCreatedAt())
                .questions(questions)
                .build();
    }

    private QuestionResponseDto toQuestionResponse(QuestionEntity question, boolean includeAnswer) {
        return QuestionResponseDto.builder()
                .id(question.getId())
                .quizId(question.getQuizId())
                .questionText(question.getQuestionText())
                .type(question.getType())
                .options(question.getOptions())
                .correctAnswer(includeAnswer ? question.getCorrectAnswer() : null)
                .explanation(includeAnswer ? question.getExplanation() : null)
                .points(question.getPoints())
                .build();
    }

    private QuizAttemptResponseDto toAttemptResponse(QuizAttemptEntity attempt, List<QuestionEntity> questions) {
        Map<UUID, QuestionEntity> questionById = questions.stream()
                .collect(Collectors.toMap(QuestionEntity::getId, Function.identity()));
        List<QuizAnswerResultResponseDto> answers = quizAnswerRepository.findByAttemptIdAndDeletedFalse(attempt.getId())
                .stream()
                .map(answer -> {
                    QuestionEntity question = questionById.get(answer.getQuestionId());
                    return QuizAnswerResultResponseDto.builder()
                            .questionId(answer.getQuestionId())
                            .selectedAnswer(answer.getSelectedAnswer())
                            .correct(answer.getIsCorrect())
                            .earnedPoints(answer.getEarnedPoints())
                            .points(question == null ? null : question.getPoints())
                            .correctAnswer(question == null ? null : question.getCorrectAnswer())
                            .explanation(question == null ? null : question.getExplanation())
                            .build();
                })
                .toList();
        return QuizAttemptResponseDto.builder()
                .id(attempt.getId())
                .studentId(attempt.getStudentId())
                .quizId(attempt.getQuizId())
                .attemptNumber(attempt.getAttemptNumber())
                .score(attempt.getScore())
                .totalPoints(attempt.getTotalPoints())
                .earnedPoints(attempt.getEarnedPoints())
                .passed(attempt.getPassed())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .timeSpent(attempt.getTimeSpent())
                .answers(answers)
                .build();
    }
}
