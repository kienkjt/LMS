package com.kjt.lms.service;

import com.kjt.lms.model.request.chapter.CreateChapterRequestDto;
import com.kjt.lms.model.request.chapter.UpdateChapterRequestDto;
import com.kjt.lms.model.response.ChapterResponseDto;

import java.util.List;
import java.util.UUID;

public interface ChapterService {

    ChapterResponseDto createChapter(UUID courseId, CreateChapterRequestDto request);

    ChapterResponseDto getChapterById(UUID courseId, UUID chapterId);

    List<ChapterResponseDto> getChaptersByCourse(UUID courseId);

    ChapterResponseDto updateChapter(UUID courseId, UUID chapterId, UpdateChapterRequestDto request);

    void deleteChapter(UUID courseId, UUID chapterId);
}
