package com.kjt.lms.model.response.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class PlatformFeeResponseDto {
    private BigDecimal platformFeePercent;
    private BigDecimal instructorRevenuePercent;
}
