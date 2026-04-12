package com.kjt.lms.controller;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.request.category.CreateCategoryRequestDto;
import com.kjt.lms.model.request.category.UpdateCategoryRequestDto;
import com.kjt.lms.model.response.category.CategoryResponseDto;
import com.kjt.lms.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category management endpoints")
public class CategoryController {

    private final CategoryService categoryService;
    private final MessageProvider messageProvider;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create category", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<CategoryResponseDto>> createCategory(
            @Valid @RequestBody CreateCategoryRequestDto request) {
        CategoryResponseDto response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.success(response, messageProvider.getMessage("category.created.success")));
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<APIResponse<CategoryResponseDto>> getCategoryById(
            @PathVariable UUID categoryId) {
        CategoryResponseDto response = categoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @GetMapping
    @Operation(summary = "Get categories")
    public ResponseEntity<APIResponse<Page<CategoryResponseDto>>> getCategories(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CategoryResponseDto> response = categoryService.getCategories(keyword, pageable);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update category", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<CategoryResponseDto>> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequestDto request) {
        CategoryResponseDto response = categoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("category.updated.success")));
    }

    @PostMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete category", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Void>> deleteCategory(
            @PathVariable UUID categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(APIResponse.success(null, messageProvider.getMessage("category.deleted.success")));
    }
}

