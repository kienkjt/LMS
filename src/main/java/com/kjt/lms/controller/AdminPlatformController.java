package com.kjt.lms.controller;

import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.request.admin.UpdatePlatformFeeRequestDto;
import com.kjt.lms.model.response.admin.PlatformFeeResponseDto;
import com.kjt.lms.service.PlatformFeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/platform")
@RequiredArgsConstructor
@Tag(name = "Admin Platform", description = "Platform settings for admin")
public class AdminPlatformController {

    private final PlatformFeeService platformFeeService;

    @GetMapping("/fee")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get platform fee", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<PlatformFeeResponseDto>> getPlatformFee() {
        return ResponseEntity.ok(APIResponse.success(platformFeeService.getPlatformFee(), null));
    }

    @PutMapping("/fee")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update platform fee", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<PlatformFeeResponseDto>> updatePlatformFee(
            @Valid @RequestBody UpdatePlatformFeeRequestDto request) {
        return ResponseEntity.ok(APIResponse.success(
                platformFeeService.updatePlatformFee(request.getPlatformFeePercent()),
                "Cap nhat phi nen tang thanh cong"
        ));
    }
}
