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
public class UserDetailResponse {
    private UUID id;
    private String email;
    private String fullName;
    private GenderEnum gender;
    private String phoneNumber;
    private String avatar;
    private String bio;
    private String roleName; // Tên vai trò
    private String roleCode; // Code vai trò
    private CommonStatusEnum active; // Trạng thái hoạt động
    private Boolean isLocked; // Tình trạng tài khoản
    private Boolean isVerified; // Trạng thái xác thực
    private BigDecimal totalRevenue; // Tổng doanh thu (cho Teacher)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

