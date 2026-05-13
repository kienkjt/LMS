package com.kjt.lms.controller;

import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.response.dashboard.AdminReportResponseDto;
import com.kjt.lms.model.response.dashboard.InstructorReportResponseDto;
import com.kjt.lms.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Admin and instructor report statistics")
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get admin report statistics by days", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<AdminReportResponseDto>> getAdminReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "30") Integer days) {
        return ResponseEntity.ok(APIResponse.success(reportService.getAdminReport(year, month, days), null));
    }

    @GetMapping("/instructor")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Get instructor report statistics by days", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<InstructorReportResponseDto>> getInstructorReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "30") Integer days) {
        return ResponseEntity.ok(APIResponse.success(reportService.getInstructorReport(year, month, days), null));
    }
}
