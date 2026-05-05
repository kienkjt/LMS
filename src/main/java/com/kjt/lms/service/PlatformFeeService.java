package com.kjt.lms.service;

import com.kjt.lms.model.response.admin.PlatformFeeResponseDto;

import java.math.BigDecimal;

public interface PlatformFeeService {
    BigDecimal getInstructorRevenueRate();
    PlatformFeeResponseDto getPlatformFee();
    PlatformFeeResponseDto updatePlatformFee(BigDecimal platformFeePercent);
}
