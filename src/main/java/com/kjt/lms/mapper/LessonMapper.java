package com.kjt.lms.mapper;

import com.kjt.lms.common.constants.YesNoEnum;
import com.kjt.lms.model.entity.LessonEntity;
import com.kjt.lms.model.request.lesson.CreateLessonRequestDto;
import com.kjt.lms.model.request.lesson.UpdateLessonRequestDto;
import com.kjt.lms.model.response.lesson.LessonResponseDto;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.UUID;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface LessonMapper {

    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "chapterId", source = "chapterId")
    @Mapping(target = "freePreview", expression = "java(toYesNoEnum(request.getFreePreview()))")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedById", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    LessonEntity toCreateEntity(CreateLessonRequestDto request, UUID courseId, UUID chapterId);

    @Mapping(target = "freePreview", expression = "java(entity.getFreePreview() == com.kjt.lms.common.constants.YesNoEnum.ACTIVE)")
    LessonResponseDto toDto(LessonEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedById", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "courseId", ignore = true)
    @Mapping(target = "chapterId", ignore = true)
    @Mapping(target = "freePreview", expression = "java(toYesNoEnum(request.getFreePreview()))")
    void updateEntity(UpdateLessonRequestDto request, @MappingTarget LessonEntity entity);

    default YesNoEnum toYesNoEnum(Boolean freePreview) {
        return Boolean.TRUE.equals(freePreview)
                ? YesNoEnum.ACTIVE
                : YesNoEnum.INACTIVE;
    }
}
