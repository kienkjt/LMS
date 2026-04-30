package com.kjt.lms.service;

import com.kjt.lms.common.constants.WithdrawalStatusEnum;
import com.kjt.lms.model.request.withdrawal.CreateWithdrawalRequestDto;
import com.kjt.lms.model.response.withdrawal.InstructorWalletResponseDto;
import com.kjt.lms.model.response.withdrawal.WithdrawalRequestResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface WithdrawalService {

    void addEarnings(UUID instructorId, UUID orderId, BigDecimal amount);

    void deductBalance(UUID instructorId, BigDecimal amount);

    WithdrawalRequestResponseDto createWithdrawalRequest(CreateWithdrawalRequestDto request);

    WithdrawalRequestResponseDto getWithdrawalRequest(UUID requestId);

    Page<WithdrawalRequestResponseDto> getInstructorWithdrawals(Pageable pageable);

    Page<WithdrawalRequestResponseDto> getPendingWithdrawals(Pageable pageable);

    WithdrawalRequestResponseDto approveWithdrawal(UUID requestId);

    WithdrawalRequestResponseDto rejectWithdrawal(UUID requestId, String rejectReason);

    WithdrawalRequestResponseDto cancelWithdrawalRequest(UUID requestId);

    WithdrawalRequestResponseDto completeWithdrawal(UUID requestId, String transactionId);

    InstructorWalletResponseDto getInstructorWallet();

    void processRefundAdjustment(UUID orderId);

    int releasePendingEarnings();

    Page<WithdrawalRequestResponseDto> getAllWithdrawals(Pageable pageable);

    Page<WithdrawalRequestResponseDto> getWithdrawalsByStatus(WithdrawalStatusEnum status, Pageable pageable);
}

