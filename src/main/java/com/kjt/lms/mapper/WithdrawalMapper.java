package com.kjt.lms.mapper;

import com.kjt.lms.model.entity.InstructorWalletEntity;
import com.kjt.lms.model.entity.WithdrawalRequestEntity;
import com.kjt.lms.model.response.withdrawal.InstructorWalletResponseDto;
import com.kjt.lms.model.response.withdrawal.WithdrawalRequestResponseDto;
import org.springframework.stereotype.Component;

@Component
public class WithdrawalMapper {

    public WithdrawalRequestResponseDto toResponse(WithdrawalRequestEntity entity) {
        if (entity == null) {
            return null;
        }

        return WithdrawalRequestResponseDto.builder()
                .id(entity.getId())
                .instructorId(entity.getInstructorId())
                .orderId(entity.getOrderId())
                .type(entity.getType())
                .status(entity.getStatus())
                .requestedAmount(entity.getRequestedAmount())
                .commissionRate(entity.getCommissionRate())
                .commissionAmount(entity.getCommissionAmount())
                .netAmount(entity.getNetAmount())
                .bankAccount(entity.getBankAccount())
                .bankName(entity.getBankName())
                .accountHolder(entity.getAccountHolder())
                .reason(entity.getReason())
                .approvedAt(entity.getApprovedAt())
                .completedAt(entity.getCompletedAt())
                .rejectedAt(entity.getRejectedAt())
                .rejectReason(entity.getRejectReason())
                .transactionId(entity.getTransactionId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public InstructorWalletResponseDto toWalletResponse(InstructorWalletEntity entity) {
        if (entity == null) {
            return null;
        }

        return InstructorWalletResponseDto.builder()
                .id(entity.getId())
                .instructorId(entity.getInstructorId())
                .currentBalance(entity.getCurrentBalance())
                .availableBalance(entity.getCurrentBalance())
                .pendingWithdrawalAmount(java.math.BigDecimal.ZERO)
                .totalEarned(entity.getTotalEarned())
                .totalWithdrawn(entity.getTotalWithdrawn())
                .totalCommissionDeducted(entity.getTotalCommissionDeducted())
                .totalRefunded(entity.getTotalRefunded())
                .build();
    }
}

