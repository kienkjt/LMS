package com.kjt.lms.model.response.withdrawal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorWalletResponseDto {
    private UUID id;
    private UUID instructorId;
    private BigDecimal currentBalance;
    private BigDecimal availableBalance;
    private BigDecimal pendingWithdrawalAmount;
    private BigDecimal totalEarned;
    private BigDecimal totalWithdrawn;
    private BigDecimal totalCommissionDeducted;
    private BigDecimal totalRefunded;
}

