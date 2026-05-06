package com.kjt.lms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.model.request.ai.LearningAssistantPromptRequestDto;
import com.kjt.lms.model.response.ai.LearningAssistantPromptResponseDto;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningAssistantServiceImpl implements LearningAssistantService {

    private static final String PROMPT_TEMPLATE = """
            Ban la tro ly hoc tap AI cua he thong LMS.

            Muc tieu:
            - Tra loi cau hoi hoc tap dua tren Context.
            - Voi cau hoi don gian: tra loi ngan gon, trong tam.
            - Voi cau hoi phuc tap: giai thich theo tung buoc, co cau truc ro rang.

            QUY TAC BAT BUOC:
            1) CHI su dung thong tin nam trong Context.
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
        String question = normalize(request.getUserQuestion());
        String context = limitText(normalize(request.getRetrievedChunks()), Math.max(maxContextChars, 2000));
        boolean complex = isComplexQuestion(question);
        String answerMode = complex ? "COMPLEX" : "SIMPLE";
        String prompt = PROMPT_TEMPLATE.formatted(
                answerMode,
                normalize(request.getStudentName()),
                normalize(request.getCourseName()),
                request.getProgressPercent() == null ? 0 : request.getProgressPercent(),
                normalize(request.getCurrentLesson()),
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
}
