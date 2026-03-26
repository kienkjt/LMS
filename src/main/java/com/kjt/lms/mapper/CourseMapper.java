package com.kjt.lms.mapper;

import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.response.CourseResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    @Mapping(source = "createdById", target = "createdById")
    @Mapping(source = "updatedById", target = "updatedById")
    CourseResponseDto toDto(CourseEntity entity);

    CourseEntity toEntity(CourseResponseDto dto);
}
