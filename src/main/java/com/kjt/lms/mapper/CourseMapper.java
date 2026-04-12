package com.kjt.lms.mapper;

import com.kjt.lms.model.entity.ChapterEntity;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.LessonEntity;
import com.kjt.lms.model.request.course.CreateCourseRequestDto;
import com.kjt.lms.model.request.course.UpdateCourseRequestDto;
import com.kjt.lms.model.response.chapter.ChapterResponseDto;
import com.kjt.lms.model.response.course.CourseCreateResponseDto;
import com.kjt.lms.model.response.course.CourseDetailResponseDto;
import com.kjt.lms.model.response.course.CourseListItemResponseDto;
import com.kjt.lms.model.response.course.CourseUpdateResponseDto;
import com.kjt.lms.model.response.lesson.LessonResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    @Mapping(source = "createdById", target = "createdById")
    @Mapping(source = "updatedById", target = "updatedById")
    CourseUpdateResponseDto toDto(CourseEntity entity);

    CourseCreateResponseDto toCreateResponse(CourseEntity entity);

    CourseListItemResponseDto toListItemDto(CourseEntity entity);

    @Mapping(target = "instructorId", source = "instructorId")
    @Mapping(target = "status", expression = "java(com.kjt.lms.common.constants.CourseStatusEnum.DRAFT)")
    @Mapping(target = "totalLessons", constant = "0")
    @Mapping(target = "totalStudents", constant = "0")
    @Mapping(target = "avgRating", constant = "0.0")
    @Mapping(target = "totalReviews", constant = "0")
    @Mapping(target = "active", expression = "java(com.kjt.lms.common.constants.CommonStatusEnum.ACTIVE)")
    @Mapping(target = "reviewedBy", ignore = true)
    @Mapping(target = "rejectReason", ignore = true)
    CourseEntity toCreateEntity(CreateCourseRequestDto request, UUID instructorId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "instructorId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalLessons", ignore = true)
    @Mapping(target = "totalStudents", ignore = true)
    @Mapping(target = "avgRating", ignore = true)
    @Mapping(target = "totalReviews", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "reviewedBy", ignore = true)
    @Mapping(target = "rejectReason", ignore = true)
    void updateCourseFromRequest(UpdateCourseRequestDto request, @MappingTarget CourseEntity course);

    CourseDetailResponseDto toDetailDto(CourseEntity entity);

    @Mapping(target = "lessons", ignore = true)
    ChapterResponseDto toChapterDto(ChapterEntity entity);

    @Mapping(target = "freePreview", expression = "java(entity.getFreePreview() == com.kjt.lms.common.constants.YesNoEnum.ACTIVE)")
    LessonResponseDto toLessonDto(LessonEntity entity);
}
