package com.kjt.lms.mapper;

import com.kjt.lms.model.entity.ChapterEntity;
import com.kjt.lms.model.request.chapter.CreateChapterRequestDto;
import com.kjt.lms.model.request.chapter.UpdateChapterRequestDto;
import com.kjt.lms.model.response.ChapterResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ChapterMapper {

    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "totalLessons", constant = "0")
    @Mapping(target = "totalDuration", constant = "0")
    ChapterEntity toCreateEntity(CreateChapterRequestDto request, UUID courseId);

    @Mapping(target = "lessons", ignore = true)
    ChapterResponseDto toDto(ChapterEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "courseId", ignore = true)
    @Mapping(target = "totalLessons", ignore = true)
    @Mapping(target = "totalDuration", ignore = true)
    void updateEntity(UpdateChapterRequestDto request, @MappingTarget ChapterEntity entity);
}
