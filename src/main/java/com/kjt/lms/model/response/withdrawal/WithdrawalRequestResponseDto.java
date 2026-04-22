package com.kjt.lms.model.response.withdrawal;

import com.kjt.lms.common.constants.WithdrawalStatusEnum;
import com.kjt.lms.common.constants.WithdrawalTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRequestResponseDto {
    private UUID id;
    private UUID instructorId;
    private UUID orderId;
    private WithdrawalTypeEnum type;
    private WithdrawalStatusEnum status;
    private BigDecimal requestedAmount;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private BigDecimal netAmount;
    private String bankAccount;
    private String bankName;
    private String accountHolder;
    private String reason;
    private LocalDateTime approvedAt;
    private LocalDateTime completedAt;
    private LocalDateTime rejectedAt;
    private String rejectReason;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

