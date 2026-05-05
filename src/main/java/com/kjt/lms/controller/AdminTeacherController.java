package com.kjt.lms.controller;

import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.request.admin.ListUserRequest;
import com.kjt.lms.model.response.admin.UserListResponse;
import com.kjt.lms.model.response.admin.UserDetailResponse;
import com.kjt.lms.service.AdminTeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/teachers")
@RequiredArgsConstructor
@Tag(name = "Admin Teacher Management", description = "Quản lý giáo viên cho admin")
public class AdminTeacherController {

    private final AdminTeacherService adminTeacherService;
    private final MessageProvider messageProvider;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách giáo viên", description = "Lấy danh sách tất cả giáo viên với tìm kiếm và lọc", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Page<UserListResponse>>> listTeachers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String active,
            @RequestParam(required = false) Boolean isLocked,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        ListUserRequest request = ListUserRequest.builder()
                .keyword(keyword)
                .roleCode("TEACHER")
                .active(active != null ? com.kjt.lms.common.constants.CommonStatusEnum.valueOf(active) : null)
                .isLocked(isLocked)
                .page(Math.max(page, 0))
                .size(Math.max(size, 1))
                .build();

        Page<UserListResponse> teachers = adminTeacherService.listTeachers(request);
        return ResponseEntity.ok(APIResponse.success(teachers, null));
    }

    @GetMapping("/{teacherId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Chi tiết giáo viên", description = "Lấy thông tin chi tiết của một giáo viên bao gồm số khóa học, học viên và doanh thu", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Map<String, Object>>> getTeacherDetail(@PathVariable UUID teacherId) {
        UserDetailResponse teacher = adminTeacherService.getTeacherDetail(teacherId);
        long courseCount = adminTeacherService.getTeacherCourseCount(teacherId);
        long studentCount = adminTeacherService.getTeacherStudentCount(teacherId);
        BigDecimal totalRevenue = adminTeacherService.getTeacherTotalRevenue(teacherId);

        Map<String, Object> response = new HashMap<>();
        response.put("user", teacher);
        response.put("courseCount", courseCount);
        response.put("studentCount", studentCount);
        response.put("totalRevenue", totalRevenue);

        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @GetMapping("/{teacherId}/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thống kê giáo viên", description = "Lấy thống kê chi tiết về khóa học, học viên và doanh thu", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Map<String, Object>>> getTeacherStatistics(@PathVariable UUID teacherId) {
        long courseCount = adminTeacherService.getTeacherCourseCount(teacherId);
        long studentCount = adminTeacherService.getTeacherStudentCount(teacherId);
        BigDecimal totalRevenue = adminTeacherService.getTeacherTotalRevenue(teacherId);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("courseCount", courseCount);
        statistics.put("studentCount", studentCount);
        statistics.put("totalRevenue", totalRevenue);
        statistics.put("teacherId", teacherId);

        return ResponseEntity.ok(APIResponse.success(statistics, null));
    }
}


