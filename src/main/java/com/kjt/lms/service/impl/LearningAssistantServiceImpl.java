package com.kjt.lms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.security.SecurityUtils;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.EnrollmentEntity;
import com.kjt.lms.model.entity.LessonEntity;
import com.kjt.lms.model.entity.LessonProgressEntity;
import com.kjt.lms.model.entity.UserEntity;
import com.kjt.lms.model.request.ai.LearningAssistantPromptRequestDto;
import com.kjt.lms.model.response.ai.LearningAssistantPromptResponseDto;
import com.kjt.lms.repository.CategoryRepository;
import com.kjt.lms.repository.CourseRepository;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.LessonProgressRepository;
import com.kjt.lms.repository.LessonRepository;
import com.kjt.lms.repository.UserRepository;
import com.kjt.lms.service.LearningAssistantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningAssistantServiceImpl implements LearningAssistantService {

    private static final String PROMPT_TEMPLATE = """
            Ban la tro ly hoc tap AI cua he thong LMS.

            Muc tieu:
            - Tra loi cau hoi hoc tap dua tren Context.
            - Duoc phep su dung context he thong LMS (ho so hoc vien, khoa hoc, tien do) de tra loi sat thuc te.
            - Voi cau hoi don gian: tra loi ngan gon, trong tam.
            - Voi cau hoi phuc tap: giai thich theo tung buoc, co cau truc ro rang.

            QUY TAC BAT BUOC:
            1) CHI su dung thong tin nam trong Context (bao gom context nguoi dung gui len va context he thong LMS).
            2) Khong suy dien vuot qua Context.
            3) Neu Context thieu du lieu de ket luan, bat buoc tra loi:
               "Toi chua tim thay thong tin phu hop trong he thong khoa hoc."
               Sau do de xuat nguoi hoc can cung cap them thong tin nao.
            4) Uu tien tieng Viet, de hieu, chinh xac.
            5) Neu cau hoi ngoai pham vi hoc tap LMS (chinh tri, y te, tai chinh, noi dung khong lien quan), lich su tu choi.
            6) Neu cau hoi mo ho hoac thieu thong tin, dat toi da 2 cau hoi lam ro truoc khi ket luan.
            7) Van phong than thien, chuyen nghiep; tra loi ngan gon neu nguoi dung hoi ngan.

            CHE DO TRA LOI: %s
            - SIMPLE: 1 doan ngan + vi du ngan (neu can)
            - COMPLEX: tra loi theo cac muc:
              (a) Ket luan chinh
              (b) Phan tich theo tung buoc
              (c) Goi y hanh dong tiep theo cho hoc vien

            Thong tin hoc vien:
            - Ten: %s
            - Khoa hoc hien tai: %s
            - Tien do hoan thanh: %d%%
            - Bai hoc hien tai: %s

            Lich su hoi thoai gan day:
            %s

            Context:
            %s

            Cau hoi cua hoc vien:
            %s
            """;

    private static final int MIN_COMPLEX_QUESTION_LENGTH = 120;
    private static final List<String> COMPLEX_KEYWORDS = List.of(
            "tai sao", "vi sao", "so sanh", "phan tich", "chung minh",
            "thuat toan", "toi uu", "trade-off", "kien truc", "he thong",
            "multi", "nhieu buoc", "step by step", "chi tiet"
    );

    private final ObjectMapper objectMapper;
    private final MessageProvider messageProvider;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBaseUrl;

    @Value("${gemini.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${gemini.advanced-model:gemini-2.5-pro}")
    private String geminiAdvancedModel;

    @Value("${gemini.temperature.simple:0.3}")
    private double simpleTemperature;

    @Value("${gemini.temperature.complex:0.2}")
    private double complexTemperature;

    @Value("${gemini.max-output-tokens:1024}")
    private int maxOutputTokens;

    @Value("${gemini.max-context-chars:12000}")
    private int maxContextChars;

    @Override
    public LearningAssistantPromptResponseDto askAssistant(LearningAssistantPromptRequestDto request) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        String question = normalize(request.getUserQuestion());
        AssistantContextData assistantContextData = buildAssistantContext(currentUserId, request);
        AssistantIntent intent = detectIntent(question);
        List<LearningAssistantPromptResponseDto.RecommendedCourseDto> recommendedCourses = recommendCourses(request, intent);
        List<LearningAssistantPromptResponseDto.RoadmapStepDto> roadmap = buildRoadmap(request, recommendedCourses, intent);
        List<String> followUpQuestions = buildFollowUpQuestions(request, intent);
        String context = limitText(assistantContextData.context(), Math.max(maxContextChars, 2000));
        String chatHistory = limitText(buildChatHistoryContext(request.getChatHistory()), 2000);
        String recommendationContext = buildRecommendationContext(recommendedCourses, roadmap, intent, request);
        boolean complex = isComplexQuestion(question);
        String answerMode = complex ? "COMPLEX" : "SIMPLE";
        String prompt = PROMPT_TEMPLATE.formatted(
                answerMode,
                assistantContextData.studentName(),
                assistantContextData.courseName(),
                assistantContextData.progressPercent(),
                assistantContextData.currentLesson(),
                chatHistory,
                context,
                question
        ) + "\n\nThong tin goi y bo sung:\n" + recommendationContext;

        String answer = generateAnswerWithFallback(prompt, complex, intent, recommendedCourses, roadmap, followUpQuestions);

        return LearningAssistantPromptResponseDto.builder()
                .intent(intent.name())
                .prompt(prompt)
                .answer(answer)
                .followUpQuestions(followUpQuestions)
                .recommendedCourses(recommendedCourses)
                .roadmap(roadmap)
                .build();
    }

    private String generateAnswerWithFallback(
            String prompt,
            boolean complexMode,
            AssistantIntent intent,
            List<LearningAssistantPromptResponseDto.RecommendedCourseDto> recommendedCourses,
            List<LearningAssistantPromptResponseDto.RoadmapStepDto> roadmap,
            List<String> followUpQuestions
    ) {
        String apiKey = normalize(geminiApiKey);
        if (!apiKey.isEmpty()) {
            try {
                return callGemini(prompt, complexMode, apiKey);
            } catch (RuntimeException ex) {
                log.warn("Falling back to deterministic assistant answer due to AI provider error: {}", ex.getMessage());
            }
        }
        return buildDeterministicAnswer(intent, recommendedCourses, roadmap, followUpQuestions);
    }

    private String callGemini(String prompt, boolean complexMode, String apiKey) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("contents", List.of(
                    Map.of("parts", List.of(Map.of("text", prompt)))
            ));
            payload.put("generationConfig", Map.of(
                    "temperature", complexMode ? complexTemperature : simpleTemperature,
                    "topP", 0.9,
                    "maxOutputTokens", Math.max(256, maxOutputTokens)
            ));

            String body = objectMapper.writeValueAsString(payload);
            String model = resolveModel(complexMode);
            String url = normalize(geminiBaseUrl) + "/models/" + model + ":generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.max(timeoutSeconds, 10)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(Math.max(timeoutSeconds, 10)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Gemini API failed with status {} and body {}", response.statusCode(), response.body());
                throw new IllegalStateException(messageProvider.getMessage("exception.ai.unavailable"));
            }

            return extractAnswer(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Gemini API call interrupted", ex);
            throw new IllegalStateException(messageProvider.getMessage("exception.ai.unavailable"));
        } catch (IOException ex) {
            log.error("Gemini API call failed", ex);
            throw new IllegalStateException(messageProvider.getMessage("exception.ai.unavailable"));
        }
    }

    private String extractAnswer(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");

        if (candidates.isArray()) {
            for (JsonNode candidate : candidates) {
                JsonNode parts = candidate.path("content").path("parts");
                if (!parts.isArray()) {
                    continue;
                }

                StringBuilder sb = new StringBuilder();
                for (JsonNode part : parts) {
                    String text = part.path("text").asText("");
                    if (!text.isBlank()) {
                        if (sb.length() > 0) {
                            sb.append("\n");
                        }
                        sb.append(text.trim());
                    }
                }

                if (sb.length() > 0) {
                    return sb.toString();
                }
            }
        }

        throw new IllegalStateException(messageProvider.getMessage("exception.ai.invalidResponse"));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String buildChatHistoryContext(List<LearningAssistantPromptRequestDto.ChatMessageDto> chatHistory) {
        if (chatHistory == null || chatHistory.isEmpty()) {
            return "Khong co";
        }

        return chatHistory.stream()
                .filter(message -> message != null && !normalize(message.getContent()).isEmpty())
                .limit(8)
                .map(message -> {
                    String role = normalize(message.getRole()).toLowerCase();
                    String roleLabel = switch (role) {
                        case "assistant", "ai", "bot" -> "Tro ly";
                        case "system" -> "He thong";
                        default -> "Hoc vien";
                    };
                    return "- " + roleLabel + ": " + normalize(message.getContent());
                })
                .collect(Collectors.joining("\n"));
    }

    private AssistantContextData buildAssistantContext(UUID currentUserId, LearningAssistantPromptRequestDto request) {
        UserEntity user = userRepository.findById(currentUserId).orElse(null);
        String studentName = normalize(request.getStudentName());
        if (studentName.isEmpty() && user != null) {
            studentName = normalize(user.getFullName());
        }

        Optional<EnrollmentEntity> selectedEnrollment = resolveSelectedEnrollment(currentUserId, request.getCourseId());
        CourseEntity selectedCourse = selectedEnrollment
                .flatMap(enrollment -> courseRepository.findByIdAndDeletedFalse(enrollment.getCourseId()))
                .orElse(null);

        String courseName = normalize(request.getCourseName());
        if (courseName.isEmpty() && selectedCourse != null) {
            courseName = normalize(selectedCourse.getTitle());
        }

        int progressPercent = request.getProgressPercent() == null ? -1 : request.getProgressPercent();
        if (progressPercent < 0 && selectedEnrollment.isPresent() && selectedEnrollment.get().getProgressPercent() != null) {
            progressPercent = selectedEnrollment.get().getProgressPercent().intValue();
        }
        if (progressPercent < 0) {
            progressPercent = 0;
        }

        String currentLesson = normalize(request.getCurrentLesson());
        if (currentLesson.isEmpty()) {
            currentLesson = resolveCurrentLessonName(currentUserId, selectedEnrollment.map(EnrollmentEntity::getCourseId).orElse(null));
        }

        StringBuilder contextBuilder = new StringBuilder();
        String retrievedChunks = normalize(request.getRetrievedChunks());
        if (!retrievedChunks.isEmpty()) {
            contextBuilder.append("Nguon nguoi dung cung cap:\n").append(retrievedChunks).append("\n\n");
        }

        if (Boolean.TRUE.equals(request.getIncludeSystemContext())) {
            contextBuilder.append(buildSystemContext(currentUserId, selectedEnrollment, selectedCourse, progressPercent, currentLesson));
        }

        return new AssistantContextData(
                studentName.isEmpty() ? "Hoc vien" : studentName,
                courseName.isEmpty() ? "Chua xac dinh" : courseName,
                Math.max(0, Math.min(100, progressPercent)),
                currentLesson.isEmpty() ? "Chua xac dinh" : currentLesson,
                contextBuilder.toString().trim()
        );
    }

    private Optional<EnrollmentEntity> resolveSelectedEnrollment(UUID currentUserId, UUID courseId) {
        if (courseId != null) {
            return enrollmentRepository.findByStudentIdAndCourseIdAndDeletedFalse(currentUserId, courseId);
        }

        return enrollmentRepository.findByStudentIdAndDeletedFalse(currentUserId).stream()
                .max(Comparator.comparing(EnrollmentEntity::getCreatedAt));
    }

    private String resolveCurrentLessonName(UUID studentId, UUID courseId) {
        if (courseId == null) {
            return "";
        }
        List<LessonEntity> lessons = lessonRepository.findByCourseIdAndDeletedFalseOrderByCreatedAtAsc(courseId);
        if (lessons.isEmpty()) {
            return "";
        }

        Set<UUID> completedLessonIds = lessonProgressRepository.findByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId).stream()
                .filter(progress -> Boolean.TRUE.equals(progress.getCompleted()))
                .map(LessonProgressEntity::getLessonId)
                .collect(Collectors.toSet());

        return lessons.stream()
                .filter(lesson -> !completedLessonIds.contains(lesson.getId()))
                .findFirst()
                .map(LessonEntity::getTitle)
                .orElse(lessons.get(lessons.size() - 1).getTitle());
    }

    private String buildSystemContext(
            UUID currentUserId,
            Optional<EnrollmentEntity> selectedEnrollment,
            CourseEntity selectedCourse,
            int progressPercent,
            String currentLesson
    ) {
        StringBuilder systemContext = new StringBuilder();
        systemContext.append("Nguon he thong LMS:\n");

        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStudentIdAndDeletedFalse(currentUserId);
        systemContext.append("- Tong so khoa hoc da ghi danh: ").append(enrollments.size()).append("\n");

        Set<String> enrolledCourseTitles = new LinkedHashSet<>();
        for (EnrollmentEntity enrollment : enrollments.stream().limit(5).toList()) {
            courseRepository.findByIdAndDeletedFalse(enrollment.getCourseId())
                    .map(CourseEntity::getTitle)
                    .ifPresent(enrolledCourseTitles::add);
        }
        if (!enrolledCourseTitles.isEmpty()) {
            systemContext.append("- Cac khoa hoc gan day: ")
                    .append(String.join(", ", enrolledCourseTitles))
                    .append("\n");
        }

        if (selectedEnrollment.isPresent() && selectedCourse != null) {
            UUID courseId = selectedEnrollment.get().getCourseId();
            long completedLessons = lessonProgressRepository
                    .countByStudentIdAndCourseIdAndCompletedTrueAndDeletedFalse(currentUserId, courseId);
            long totalLessons = lessonRepository.countByCourseIdAndDeletedFalse(courseId);
            systemContext.append("- Khoa hoc dang xet: ").append(normalize(selectedCourse.getTitle())).append("\n");
            systemContext.append("- Tien do khoa hoc: ").append(progressPercent).append("%")
                    .append(" (").append(completedLessons).append("/").append(totalLessons).append(" bai da hoan thanh)\n");
            if (!normalize(currentLesson).isEmpty()) {
                systemContext.append("- Bai hoc hien tai/goi y tiep theo: ").append(currentLesson).append("\n");
            }
        }

        // Add information about all available public courses for AI reference
        systemContext.append("\n").append(buildPublicCoursesContext());

        return systemContext.toString();
    }

    private String buildPublicCoursesContext() {
        StringBuilder coursesContext = new StringBuilder();
        coursesContext.append("Danh sach tat ca khoa hoc cong khai trong he thong:\n");

        List<CourseEntity> publicCourses = courseRepository.findAllPublicCourses(
                List.of(com.kjt.lms.common.constants.CourseStatusEnum.PUBLISHED, com.kjt.lms.common.constants.CourseStatusEnum.APPROVED),
                com.kjt.lms.common.constants.CommonStatusEnum.ACTIVE
        );

        if (publicCourses.isEmpty()) {
            coursesContext.append("Khong co khoa hoc nao.");
            return coursesContext.toString();
        }

        coursesContext.append("Tong so: ").append(publicCourses.size()).append(" khoa hoc\n\n");

        // Group courses by category and add summary
        Map<String, List<CourseEntity>> coursesByCategory = publicCourses.stream()
                .collect(Collectors.groupingBy(course -> resolveCategoryName(course.getCategoryId())));

        for (Map.Entry<String, List<CourseEntity>> entry : coursesByCategory.entrySet()) {
            coursesContext.append("Category: ").append(entry.getKey()).append(" (").append(entry.getValue().size()).append(" courses)\n");
            for (CourseEntity course : entry.getValue().stream().limit(10).toList()) {
                coursesContext.append("  * ").append(normalize(course.getTitle()))
                        .append(" | Level: ").append(course.getLevel() == null ? "N/A" : course.getLevel().name())
                        .append(" | Price: ").append(course.getPrice())
                        .append(" | Rating: ").append(course.getAvgRating() == null ? "0" : course.getAvgRating())
                        .append(" | Students: ").append(course.getTotalStudents() == null ? "0" : course.getTotalStudents())
                        .append(" | ").append(normalize(course.getShortDescription(), 100)).append("\n");
            }
            coursesContext.append("\n");
        }

        return limitText(coursesContext.toString(), maxContextChars / 2);
    }

    private String normalize(String value, int maxLength) {
        String normalized = normalize(value);
        if (normalized.length() > maxLength) {
            return normalized.substring(0, maxLength) + "...";
        }
        return normalized;
    }

    private String resolveModel(boolean complexMode) {
        String defaultModel = normalize(geminiModel).isEmpty() ? "gemini-2.5-flash" : normalize(geminiModel);
        if (!complexMode) {
            return defaultModel;
        }
        String advancedModel = normalize(geminiAdvancedModel);
        return advancedModel.isEmpty() ? defaultModel : advancedModel;
    }

    private boolean isComplexQuestion(String question) {
        String normalizedQuestion = normalize(question).toLowerCase();
        if (normalizedQuestion.length() >= MIN_COMPLEX_QUESTION_LENGTH) {
            return true;
        }
        return COMPLEX_KEYWORDS.stream().anyMatch(normalizedQuestion::contains);
    }

    private String limitText(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "\n[Context da duoc cat gon do qua dai]";
    }

    private record AssistantContextData(
            String studentName,
            String courseName,
            int progressPercent,
            String currentLesson,
            String context
    ) {
    }

    private AssistantIntent detectIntent(String question) {
        String normalizedQuestion = normalize(question).toLowerCase(Locale.ROOT);
        if (containsAny(normalizedQuestion, "lo trinh", "roadmap", "ke hoach hoc", "hoc trong bao lau")) {
            return AssistantIntent.LEARNING_PATH;
        }
        if (containsAny(normalizedQuestion, "goi y", "tu van khoa hoc", "khoa hoc nao", "nen hoc khoa nao")) {
            return AssistantIntent.COURSE_RECOMMENDATION;
        }
        if (containsAny(normalizedQuestion, "gia", "hoc phi", "chinh sach", "hoan tien")) {
            return AssistantIntent.PRICE_POLICY;
        }
        return AssistantIntent.STUDY_SUPPORT;
    }

    private List<LearningAssistantPromptResponseDto.RecommendedCourseDto> recommendCourses(
            LearningAssistantPromptRequestDto request,
            AssistantIntent intent
    ) {
        if (intent == AssistantIntent.STUDY_SUPPORT) {
            return List.of();
        }

        // Fetch ALL public courses for AI assistant to analyze when user asks about courses
        List<CourseEntity> publicCourses = courseRepository.findAllPublicCourses(
                List.of(com.kjt.lms.common.constants.CourseStatusEnum.PUBLISHED, com.kjt.lms.common.constants.CourseStatusEnum.APPROVED),
                com.kjt.lms.common.constants.CommonStatusEnum.ACTIVE
        );

        String preferredCategory = normalize(request.getPreferredCategory()).toLowerCase(Locale.ROOT);
        String targetRole = normalize(request.getTargetRole()).toLowerCase(Locale.ROOT);
        BigDecimal budgetMax = request.getBudgetMax();

        return publicCourses.stream()
                .filter(course -> budgetMax == null || resolveEffectivePrice(course).compareTo(budgetMax) <= 0)
                .sorted((a, b) -> Double.compare(scoreCourse(b, preferredCategory, targetRole), scoreCourse(a, preferredCategory, targetRole)))
                .limit(5)
                .map(course -> LearningAssistantPromptResponseDto.RecommendedCourseDto.builder()
                        .courseId(course.getId())
                        .title(course.getTitle())
                        .level(course.getLevel() == null ? "UNKNOWN" : course.getLevel().name())
                        .category(resolveCategoryName(course.getCategoryId()))
                        .price(course.getPrice())
                        .discountPrice(course.getDiscountPrice())
                        .rating(course.getAvgRating())
                        .totalStudents(course.getTotalStudents())
                        .reason(buildRecommendationReason(course, preferredCategory, targetRole))
                        .build())
                .toList();
    }

    private List<LearningAssistantPromptResponseDto.RoadmapStepDto> buildRoadmap(
            LearningAssistantPromptRequestDto request,
            List<LearningAssistantPromptResponseDto.RecommendedCourseDto> recommendedCourses,
            AssistantIntent intent
    ) {
        if (intent != AssistantIntent.LEARNING_PATH && intent != AssistantIntent.COURSE_RECOMMENDATION) {
            return List.of();
        }

        String goal = normalize(request.getLearningGoal()).isEmpty() ? "Dat muc tieu hoc tap" : normalize(request.getLearningGoal());
        int weeklyHours = request.getWeeklyHours() == null ? 6 : request.getWeeklyHours();
        List<LearningAssistantPromptResponseDto.RoadmapStepDto> steps = new ArrayList<>();
        steps.add(buildRoadmapStep(1, "Nen tang", "On lai kien thuc cot loi phuc vu muc tieu: " + goal, weeklyHours, recommendedCourses, 0));
        steps.add(buildRoadmapStep(2, "Thuc hanh co huong dan", "Hoan thanh bai tap va quiz moi chuong, ghi chu cac loi gap lai", weeklyHours, recommendedCourses, 1));
        steps.add(buildRoadmapStep(3, "Du an ung dung", "Lam 1 du an mo phong tinh huong thuc te gan voi muc tieu nghe nghiep", weeklyHours, recommendedCourses, 2));
        steps.add(buildRoadmapStep(4, "Danh gia va nang cap", "Danh gia ket qua, bo sung phan yeu, dat KPI dau ra", weeklyHours, recommendedCourses, 3));
        return steps;
    }

    private LearningAssistantPromptResponseDto.RoadmapStepDto buildRoadmapStep(
            int stepNo,
            String title,
            String objective,
            int weeklyHours,
            List<LearningAssistantPromptResponseDto.RecommendedCourseDto> recommendedCourses,
            int courseIndex
    ) {
        LearningAssistantPromptResponseDto.RecommendedCourseDto courseRef = recommendedCourses.size() > courseIndex ? recommendedCourses.get(courseIndex) : null;
        int weeks = switch (stepNo) {
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 3;
            default -> 1;
        };
        return LearningAssistantPromptResponseDto.RoadmapStepDto.builder()
                .stepNo(stepNo)
                .title(title)
                .objective(objective)
                .estimatedDuration(weeks + " tuan (~" + (weeks * weeklyHours) + " gio)")
                .courseId(courseRef == null ? null : courseRef.getCourseId())
                .courseTitle(courseRef == null ? null : courseRef.getTitle())
                .build();
    }

    private List<String> buildFollowUpQuestions(LearningAssistantPromptRequestDto request, AssistantIntent intent) {
        List<String> questions = new ArrayList<>();
        if (normalize(request.getLearningGoal()).isEmpty()) {
            questions.add("Muc tieu hoc chinh cua ban trong 2-3 thang toi la gi?");
        }
        if (request.getWeeklyHours() == null) {
            questions.add("Moi tuan ban co the hoc bao nhieu gio?");
        }
        if ((intent == AssistantIntent.COURSE_RECOMMENDATION || intent == AssistantIntent.LEARNING_PATH) && request.getBudgetMax() == null) {
            questions.add("Ban muon muc hoc phi toi da cho moi khoa hoc la bao nhieu?");
        }
        return questions.stream().limit(2).toList();
    }

    private String buildRecommendationContext(
            List<LearningAssistantPromptResponseDto.RecommendedCourseDto> recommendedCourses,
            List<LearningAssistantPromptResponseDto.RoadmapStepDto> roadmap,
            AssistantIntent intent,
            LearningAssistantPromptRequestDto request
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("- Intent: ").append(intent.name()).append("\n");
        builder.append("- Learning goal: ").append(normalize(request.getLearningGoal())).append("\n");

        if (!recommendedCourses.isEmpty()) {
            builder.append("- Khoa hoc goi y:\n");
            for (LearningAssistantPromptResponseDto.RecommendedCourseDto course : recommendedCourses) {
                builder.append("  + ").append(course.getTitle())
                        .append(" | Level: ").append(course.getLevel())
                        .append(" | Category: ").append(course.getCategory())
                        .append(" | Price: ").append(course.getPrice())
                        .append(" | Rating: ").append(course.getRating())
                        .append(" | Students: ").append(course.getTotalStudents())
                        .append(" | Reason: ").append(course.getReason()).append("\n");
            }
        }

        if (!roadmap.isEmpty()) {
            builder.append("- Lo trinh goi y:\n");
            for (LearningAssistantPromptResponseDto.RoadmapStepDto step : roadmap) {
                builder.append("  + Buoc ").append(step.getStepNo()).append(": ").append(step.getTitle()).append(" - ")
                        .append(step.getObjective()).append(" (Thoi han: ").append(step.getEstimatedDuration()).append(")\n");
            }
        }
        return builder.toString().trim();
    }

    private String buildDeterministicAnswer(
            AssistantIntent intent,
            List<LearningAssistantPromptResponseDto.RecommendedCourseDto> recommendedCourses,
            List<LearningAssistantPromptResponseDto.RoadmapStepDto> roadmap,
            List<String> followUpQuestions
    ) {
        StringBuilder answer = new StringBuilder();
        answer.append("Intent: ").append(intent.name()).append(". ");
        if (!recommendedCourses.isEmpty()) {
            answer.append("Minh goi y ").append(recommendedCourses.size()).append(" khoa hoc phu hop: ");
            answer.append(recommendedCourses.stream().map(LearningAssistantPromptResponseDto.RecommendedCourseDto::getTitle).collect(Collectors.joining(", ")));
            answer.append(". ");
        }
        if (!roadmap.isEmpty()) {
            answer.append("Lo trinh hoc de xuat gom ").append(roadmap.size()).append(" buoc tu nen tang den du an ung dung. ");
        }
        if (!followUpQuestions.isEmpty()) {
            answer.append("Can them thong tin: ").append(String.join(" | ", followUpQuestions));
        }
        if (answer.toString().trim().equals("Intent: STUDY_SUPPORT.")) {
            answer.append("Ban hay gui cau hoi hoc tap cu the kem bai hoc/chuong dang hoc de minh ho tro chinh xac.");
        }
        return answer.toString().trim();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private double scoreCourse(CourseEntity course, String preferredCategory, String targetRole) {
        double score = 0.0;
        if (!preferredCategory.isEmpty() && resolveCategoryName(course.getCategoryId()).toLowerCase(Locale.ROOT).contains(preferredCategory)) {
            score += 2.0;
        }
        if (!targetRole.isEmpty()) {
            String title = normalize(course.getTitle()).toLowerCase(Locale.ROOT);
            String desc = normalize(course.getShortDescription()).toLowerCase(Locale.ROOT);
            if (title.contains(targetRole) || desc.contains(targetRole)) {
                score += 1.5;
            }
        }
        score += course.getAvgRating() == null ? 0.0 : course.getAvgRating();
        score += (course.getTotalStudents() == null ? 0 : Math.min(1000, course.getTotalStudents())) / 1000.0;
        return score;
    }

    private BigDecimal resolveEffectivePrice(CourseEntity course) {
        return course.getDiscountPrice() != null ? course.getDiscountPrice() : course.getPrice();
    }

    private String resolveCategoryName(UUID categoryId) {
        if (categoryId == null) {
            return "Unknown";
        }
        return categoryRepository.findByIdAndDeletedFalse(categoryId)
                .map(c -> normalize(c.getName()))
                .filter(name -> !name.isEmpty())
                .orElse("Unknown");
    }

    private String buildRecommendationReason(CourseEntity course, String preferredCategory, String targetRole) {
        List<String> reasons = new ArrayList<>();
        String category = resolveCategoryName(course.getCategoryId());
        if (!preferredCategory.isEmpty() && category.toLowerCase(Locale.ROOT).contains(preferredCategory)) {
            reasons.add("Dung chu de ban dang quan tam");
        }
        if (!targetRole.isEmpty()) {
            String content = (normalize(course.getTitle()) + " " + normalize(course.getShortDescription())).toLowerCase(Locale.ROOT);
            if (content.contains(targetRole)) {
                reasons.add("Noi dung lien quan muc tieu nghe nghiep");
            }
        }
        if (course.getAvgRating() != null && course.getAvgRating() >= 4.5) {
            reasons.add("Danh gia cao tu hoc vien");
        }
        if (reasons.isEmpty()) {
            reasons.add("Phu hop de bat dau va de xay dung nen tang");
        }
        return String.join("; ", reasons);
    }

    private enum AssistantIntent {
        COURSE_RECOMMENDATION,
        STUDY_SUPPORT,
        LEARNING_PATH,
        PRICE_POLICY
    }
}
