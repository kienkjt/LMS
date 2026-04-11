package com.kjt.lms.service.impl;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.mapper.CategoryMapper;
import com.kjt.lms.model.entity.CategoryEntity;
import com.kjt.lms.model.request.category.CreateCategoryRequestDto;
import com.kjt.lms.model.request.category.UpdateCategoryRequestDto;
import com.kjt.lms.model.response.CategoryResponseDto;
import com.kjt.lms.repository.CategoryRepository;
import com.kjt.lms.repository.CourseRepository;
import com.kjt.lms.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;
    private final CategoryMapper categoryMapper;
    private final MessageProvider messageProvider;

    @Override
    @Transactional
    public CategoryResponseDto createCategory(CreateCategoryRequestDto request) {
        if (categoryRepository.existsByNameIgnoreCaseAndDeletedFalse(request.getName())) {
            throw new BusinessException(messageProvider.getMessage("exception.category.duplicateName"));
        }

        CategoryEntity savedCategory = categoryRepository.save(categoryMapper.toCreateEntity(request));

        log.info("Category created: {}", savedCategory.getId());
        return categoryMapper.toDto(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(UUID categoryId) {
        CategoryEntity category = findActiveCategoryById(categoryId);
        return categoryMapper.toDto(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponseDto> getCategories(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return categoryRepository.findByDeletedFalse(pageable).map(categoryMapper::toDto);
        }

        return categoryRepository.search(keyword.trim(), pageable).map(categoryMapper::toDto);
    }

    @Override
    @Transactional
    public CategoryResponseDto updateCategory(UUID categoryId, UpdateCategoryRequestDto request) {
        CategoryEntity category = findActiveCategoryById(categoryId);

        if (categoryRepository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot(request.getName(), categoryId)) {
            throw new BusinessException(messageProvider.getMessage("exception.category.duplicateName"));
        }

        categoryMapper.updateEntity(request, category);
        CategoryEntity updatedCategory = categoryRepository.save(category);

        log.info("Category updated: {}", categoryId);
        return categoryMapper.toDto(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID categoryId) {
        CategoryEntity category = findActiveCategoryById(categoryId);

        if (courseRepository.existsByCategoryIdAndDeletedFalse(categoryId)) {
            throw new BusinessException(messageProvider.getMessage("exception.category.inUse"));
        }

        category.setDeleted(true);
        categoryRepository.save(category);

        log.info("Category deleted: {}", categoryId);
    }

    private CategoryEntity findActiveCategoryById(UUID categoryId) {
        return categoryRepository.findByIdAndDeletedFalse(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.category.notFound")));
    }
}

