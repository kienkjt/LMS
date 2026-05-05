package com.kjt.lms.service;

import com.kjt.lms.model.request.admin.ListUserRequest;
import com.kjt.lms.model.request.admin.UpdateUserStatusRequest;
import com.kjt.lms.model.request.admin.LockUserRequest;
import com.kjt.lms.model.response.admin.UserListResponse;
import com.kjt.lms.model.response.admin.UserDetailResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface AdminUserService {
    /**
     * Lấy danh sách người dùng với tìm kiếm và lọc
     */
    Page<UserListResponse> listUsers(ListUserRequest request);

    /**
     * Lấy chi tiết một người dùng
     */
    UserDetailResponse getUserDetail(UUID userId);

    /**
     * Cập nhật trạng thái hoạt động của người dùng
     */
    UserDetailResponse updateUserStatus(UUID userId, UpdateUserStatusRequest request);

    /**
     * Khóa/Mở khóa tài khoản người dùng
     */
    UserDetailResponse lockUser(UUID userId, LockUserRequest request);

    /**
     * Xóa người dùng (soft delete)
     */
    void deleteUser(UUID userId);
}

