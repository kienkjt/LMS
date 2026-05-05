package com.kjt.lms.model.response.admin;

import com.kjt.lms.common.constants.CommonStatusEnum;
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
public class TeacherDetailResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String avatar;
    private String bio;
    private CommonStatusEnum active;
    private Boolean isLocked;
    private Boolean isVerified;
    private BigDecimal totalRevenue;
    private long courseCount; // Số lượng khóa học
    private long studentCount; // Số lượng học viên
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

