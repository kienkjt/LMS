package com.kjt.lms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.security.SecurityUtils;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.EnrollmentEntity;
import com.kjt.lms.model.entity.LessonEntity;
import com.kjt.lms.model.entity.LessonProgressEntity;
import com.kjt.lms.model.entity.UserEntity;
import com.kjt.lms.model.request.ai.LearningAssistantPromptRequestDto;
import com.kjt.lms.model.response.ai.LearningAssistantPromptResponseDto;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBaseUrl;

    @Value("${gemini.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${gemini.advanced-model:gemini-1.5-pro}")
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
        String context = limitText(assistantContextData.context(), Math.max(maxContextChars, 2000));
        boolean complex = isComplexQuestion(question);
        String answerMode = complex ? "COMPLEX" : "SIMPLE";
        String prompt = PROMPT_TEMPLATE.formatted(
                answerMode,
                assistantContextData.studentName(),
                assistantContextData.courseName(),
                assistantContextData.progressPercent(),
                assistantContextData.currentLesson(),
                context,
                question
        );

        String answer = callGemini(prompt, complex);

        return LearningAssistantPromptResponseDto.builder()
                .prompt(prompt)
                .answer(answer)
                .build();
    }

    private String callGemini(String prompt, boolean complexMode) {
        String apiKey = normalize(geminiApiKey);
        if (apiKey.isEmpty()) {
            throw new BusinessException(messageProvider.getMessage("exception.ai.missingApiKey"));
        }

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
                throw new BusinessException(messageProvider.getMessage("exception.ai.unavailable"));
            }

            return extractAnswer(response.body());
        } catch (BusinessException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Gemini API call interrupted", ex);
            throw new BusinessException(messageProvider.getMessage("exception.ai.unavailable"));
        } catch (IOException ex) {
            log.error("Gemini API call failed", ex);
            throw new BusinessException(messageProvider.getMessage("exception.ai.unavailable"));
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

        throw new BusinessException(messageProvider.getMessage("exception.ai.invalidResponse"));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
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

        return systemContext.toString();
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
}
