package com.kjt.lms.controller;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.request.admin.ListUserRequest;
import com.kjt.lms.model.request.admin.UpdateUserStatusRequest;
import com.kjt.lms.model.request.admin.LockUserRequest;
import com.kjt.lms.model.response.admin.UserListResponse;
import com.kjt.lms.model.response.admin.UserDetailResponse;
import com.kjt.lms.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "Quản lý người dùng cho admin")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final MessageProvider messageProvider;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách người dùng", description = "Lấy danh sách người dùng với tìm kiếm và lọc", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Page<UserListResponse>>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) String active,
            @RequestParam(required = false) Boolean isLocked,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        ListUserRequest request = ListUserRequest.builder()
                .keyword(keyword)
                .roleCode(roleCode)
                .active(active != null ? com.kjt.lms.common.constants.CommonStatusEnum.valueOf(active) : null)
                .isLocked(isLocked)
                .page(Math.max(page, 0))
                .size(Math.max(size, 1))
                .build();

        Page<UserListResponse> users = adminUserService.listUsers(request);
        return ResponseEntity.ok(APIResponse.success(users, null));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Chi tiết người dùng", description = "Lấy thông tin chi tiết của một người dùng", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<UserDetailResponse>> getUserDetail(@PathVariable UUID userId) {
        UserDetailResponse user = adminUserService.getUserDetail(userId);
        return ResponseEntity.ok(APIResponse.success(user, null));
    }

    @PutMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật trạng thái người dùng", description = "Cập nhật trạng thái hoạt động (active/inactive) của người dùng", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<UserDetailResponse>> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        UserDetailResponse user = adminUserService.updateUserStatus(userId, request);
        return ResponseEntity.ok(APIResponse.success(user, "Cập nhật trạng thái người dùng thành công"));
    }

    @PutMapping("/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Khóa/Mở khóa tài khoản", description = "Khóa hoặc mở khóa tài khoản người dùng", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<UserDetailResponse>> lockUser(
            @PathVariable UUID userId,
            @Valid @RequestBody LockUserRequest request) {
        UserDetailResponse user = adminUserService.lockUser(userId, request);
        String message = request.getIsLocked() ? "Tài khoản đã bị khóa" : "Tài khoản đã được mở khóa";
        return ResponseEntity.ok(APIResponse.success(user, message));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa người dùng", description = "Xóa (soft delete) người dùng khỏi hệ thống", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Void>> deleteUser(@PathVariable UUID userId) {
        adminUserService.deleteUser(userId);
        return ResponseEntity.ok(APIResponse.success(null, "Xóa người dùng thành công"));
    }
}


