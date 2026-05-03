package com.kjt.lms.controller;

import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.response.dashboard.AdminDashboardResponseDto;
import com.kjt.lms.model.response.dashboard.InstructorDashboardResponseDto;
import com.kjt.lms.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Admin and instructor statistics")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get admin dashboard statistics", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<AdminDashboardResponseDto>> getAdminDashboard() {
        return ResponseEntity.ok(APIResponse.success(dashboardService.getAdminDashboard(), null));
    }

    @GetMapping("/instructor")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Get instructor dashboard statistics", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<InstructorDashboardResponseDto>> getInstructorDashboard() {
        return ResponseEntity.ok(APIResponse.success(dashboardService.getInstructorDashboard(), null));
    }
}
