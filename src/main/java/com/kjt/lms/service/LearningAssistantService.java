package com.kjt.lms.service;

import com.kjt.lms.model.request.ai.LearningAssistantPromptRequestDto;
import com.kjt.lms.model.response.ai.LearningAssistantPromptResponseDto;

public interface LearningAssistantService {
    LearningAssistantPromptResponseDto askAssistant(LearningAssistantPromptRequestDto request);
}
