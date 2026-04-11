package com.kjt.lms.service;

import com.kjt.lms.model.request.category.CreateCategoryRequestDto;
import com.kjt.lms.model.request.category.UpdateCategoryRequestDto;
import com.kjt.lms.model.response.CategoryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CategoryService {

    CategoryResponseDto createCategory(CreateCategoryRequestDto request);

    CategoryResponseDto getCategoryById(UUID categoryId);

    Page<CategoryResponseDto> getCategories(String keyword, Pageable pageable);

    CategoryResponseDto updateCategory(UUID categoryId, UpdateCategoryRequestDto request);

    void deleteCategory(UUID categoryId);
}

