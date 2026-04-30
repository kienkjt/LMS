package com.kjt.lms.controller;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.common.validator.Common;
import com.kjt.lms.model.response.wishlist.WishlistResponseDto;
import com.kjt.lms.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Student wishlist management")
@PreAuthorize("hasRole('STUDENT')")
public class WishlistController {

    private final WishlistService wishlistService;
    private final MessageProvider messageProvider;

    @GetMapping
    @Operation(summary = "Get current student's wishlist", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Page<WishlistResponseDto>>> getMyWishlist(
            @RequestParam(value = "page", defaultValue = Common.PAGE_DEFAULT) Integer page,
            @RequestParam(value = "pageSize", defaultValue = Common.PAGE_SIZE_DEFAULT) Integer pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<WishlistResponseDto> response = wishlistService.getMyWishlist(pageable);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @PostMapping("/courses/{courseId}")
    @Operation(summary = "Add a course to wishlist", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<WishlistResponseDto>> addToWishlist(@PathVariable UUID courseId) {
        WishlistResponseDto response = wishlistService.addToWishlist(courseId);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("wishlist.add.success")));
    }

    @DeleteMapping("/{wishlistId}")
    @Operation(summary = "Remove a wishlist item", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Void>> removeFromWishlist(@PathVariable UUID wishlistId) {
        wishlistService.removeFromWishlist(wishlistId);
        return ResponseEntity.ok(APIResponse.success(null, messageProvider.getMessage("wishlist.remove.success")));
    }

    @DeleteMapping("/courses/{courseId}")
    @Operation(summary = "Remove a course from wishlist", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Void>> removeCourseFromWishlist(@PathVariable UUID courseId) {
        wishlistService.removeCourseFromWishlist(courseId);
        return ResponseEntity.ok(APIResponse.success(null, messageProvider.getMessage("wishlist.remove.success")));
    }

    @GetMapping("/courses/{courseId}/exists")
    @Operation(summary = "Check whether course is in wishlist", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Map<String, Boolean>>> isCourseInWishlist(@PathVariable UUID courseId) {
        boolean exists = wishlistService.isCourseInWishlist(courseId);
        return ResponseEntity.ok(APIResponse.success(Map.of("exists", exists), null));
    }
}
