package com.kjt.lms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            Ban la tro ly hoc tap AI cua he thong LMS ban khoa hoc online.

            Nhiem vu cua ban:
            - Ho tro hoc vien tra loi cac cau hoi lien quan den khoa hoc
            - Giai thich bai hoc dua tren noi dung duoc cung cap
            - Huong dan lo trinh hoc phu hop
            - Ho tro theo doi tien do hoc tap
            - Goi y bai hoc tiep theo dua tren tien do hien tai

            Quy tac bat buoc:
            1. Chi tra loi dua tren du lieu duoc cung cap trong phan Context
            2. Khong tu bia thong tin ngoai Context
            3. Neu Context khong du thong tin, phai tra loi ro:
               "Toi chua tim thay thong tin phu hop trong he thong khoa hoc."
            4. Tra loi ngan gon, de hieu, dung trong tam
            5. Uu tien tra loi bang tieng Viet
            6. Neu nguoi dung hoi ngoai pham vi hoc tap (chinh tri, y te, tai chinh, noi dung khong lien quan LMS), hay lich su tu choi

            Thong tin hoc vien:
            - Ten: %s
            - Khoa hoc hien tai: %s
            - Tien do hoan thanh: %d%%
            - Bai hoc hien tai: %s

            Context:
            %s

            Cau hoi cua hoc vien:
            %s

            Hay tra loi chinh xac va huu ich nhat.
            """;

    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBaseUrl;

    @Value("${gemini.timeout-seconds:60}")
    private int timeoutSeconds;

    @Override
    public LearningAssistantPromptResponseDto askAssistant(LearningAssistantPromptRequestDto request) {
        String prompt = PROMPT_TEMPLATE.formatted(
                normalize(request.getStudentName()),
                normalize(request.getCourseName()),
                request.getProgressPercent() == null ? 0 : request.getProgressPercent(),
                normalize(request.getCurrentLesson()),
                normalize(request.getRetrievedChunks()),
                normalize(request.getUserQuestion())
        );

        String answer = callGemini(prompt);

        return LearningAssistantPromptResponseDto.builder()
                .prompt(prompt)
                .answer(answer)
                .build();
    }

    private String callGemini(String prompt) {
        String apiKey = normalize(geminiApiKey);
        if (apiKey.isEmpty()) {
            throw new BusinessException("Thieu cau hinh GEMINI_API_KEY.");
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("contents", List.of(
                    Map.of("parts", List.of(Map.of("text", prompt)))
            ));

            String body = objectMapper.writeValueAsString(payload);
            String model = normalize(geminiModel).isEmpty() ? "gemini-1.5-flash" : normalize(geminiModel);
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
                throw new BusinessException("Khong the lay phan hoi tu AI luc nay.");
            }

            return extractAnswer(response.body());
        } catch (BusinessException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Gemini API call interrupted", ex);
            throw new BusinessException("Khong the lay phan hoi tu AI luc nay.");
        } catch (IOException ex) {
            log.error("Gemini API call failed", ex);
            throw new BusinessException("Khong the lay phan hoi tu AI luc nay.");
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

        throw new BusinessException("AI khong tra ve noi dung phu hop.");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
