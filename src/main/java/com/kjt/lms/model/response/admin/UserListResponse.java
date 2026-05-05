package com.kjt.lms.model.response.admin;

import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.constants.GenderEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserListResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String roleName; // Tên vai trò (Student, Teacher, Admin)
    private String roleCode; // Code vai trò
    private CommonStatusEnum active; // Trạng thái hoạt động
    private Boolean isLocked; // Tình trạng tài khoản
    private Boolean isVerified; // Trạng thái xác thực
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

