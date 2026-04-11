package com.kjt.lms.service;

import com.kjt.lms.model.request.chapter.CreateChapterRequestDto;
import com.kjt.lms.model.response.ChapterResponseDto;

import java.util.UUID;

public interface ChapterService {

    ChapterResponseDto createChapter(UUID courseId, CreateChapterRequestDto request);
}

