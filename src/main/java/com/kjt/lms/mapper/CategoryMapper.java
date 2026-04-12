package com.kjt.lms.mapper;

import com.kjt.lms.model.entity.CategoryEntity;
import com.kjt.lms.model.request.category.CreateCategoryRequestDto;
import com.kjt.lms.model.request.category.UpdateCategoryRequestDto;
import com.kjt.lms.model.response.category.CategoryResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponseDto toDto(CategoryEntity entity);

    CategoryEntity toCreateEntity(CreateCategoryRequestDto request);

    @Mapping(target = "id", ignore = true)
    void updateEntity(UpdateCategoryRequestDto request, @MappingTarget CategoryEntity entity);
}

