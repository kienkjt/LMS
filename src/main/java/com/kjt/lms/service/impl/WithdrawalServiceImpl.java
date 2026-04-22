package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.WithdrawalStatusEnum;
import com.kjt.lms.common.constants.WithdrawalTypeEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.security.SecurityUtils;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.mapper.WithdrawalMapper;
import com.kjt.lms.model.entity.InstructorWalletEntity;
import com.kjt.lms.model.entity.OrderItemEntity;
import com.kjt.lms.model.entity.WithdrawalRequestEntity;
import com.kjt.lms.model.request.withdrawal.CreateWithdrawalRequestDto;
import com.kjt.lms.model.response.withdrawal.InstructorWalletResponseDto;
import com.kjt.lms.model.response.withdrawal.WithdrawalRequestResponseDto;
import com.kjt.lms.repository.InstructorWalletRepository;
import com.kjt.lms.repository.OrderItemRepository;
import com.kjt.lms.repository.WithdrawalRequestRepository;
import com.kjt.lms.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalServiceImpl extends BaseService implements WithdrawalService {

    @Value("${app.withdrawal.commission-rate:10}")
    private BigDecimal defaultCommissionRate;

    private final InstructorWalletRepository walletRepository;
    private final WithdrawalRequestRepository withdrawalRepository;
    private final OrderItemRepository orderItemRepository;
    private final WithdrawalMapper withdrawalMapper;
    private final SecurityUtils securityUtils;
    private final MessageProvider messageProvider;

    @Override
    @Transactional
    public void addEarnings(UUID instructorId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        InstructorWalletEntity wallet = walletRepository.findByInstructorIdAndDeletedFalse(instructorId)
                .orElseGet(() -> createNewWallet(instructorId));

        wallet.setCurrentBalance(wallet.getCurrentBalance().add(amount));
        wallet.setTotalEarned(wallet.getTotalEarned().add(amount));

        walletRepository.save(wallet);
        log.info("Added earnings {} to instructor {}", amount, instructorId);
    }

    @Override
    @Transactional
    public void deductBalance(UUID instructorId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        InstructorWalletEntity wallet = walletRepository.findByInstructorIdAndDeletedFalse(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.wallet.notFound")));

        if (wallet.getCurrentBalance().compareTo(amount) < 0) {
            throw new BusinessException(messageProvider.getMessage("exception.wallet.insufficientBalance"));
        }

        wallet.setCurrentBalance(wallet.getCurrentBalance().subtract(amount));
        walletRepository.save(wallet);
        log.info("Deducted {} from instructor {}", amount, instructorId);
    }

    @Override
    @Transactional
    public WithdrawalRequestResponseDto createWithdrawalRequest(CreateWithdrawalRequestDto request) {
        UUID instructorId = securityUtils.getCurrentUserId();

        InstructorWalletEntity wallet = walletRepository.findByInstructorIdAndDeletedFalse(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.wallet.notFound")));

        if (wallet.getCurrentBalance().compareTo(request.getRequestedAmount()) < 0) {
            throw new BusinessException(messageProvider.getMessage("exception.wallet.insufficientBalance"));
        }

        BigDecimal commissionAmount = calculateCommission(request.getRequestedAmount(), defaultCommissionRate);
        BigDecimal netAmount = request.getRequestedAmount().subtract(commissionAmount);

        WithdrawalRequestEntity withdrawalRequest = WithdrawalRequestEntity.builder()
                .instructorId(instructorId)
                .type(WithdrawalTypeEnum.EARNINGS)
                .status(WithdrawalStatusEnum.PENDING)
                .requestedAmount(request.getRequestedAmount())
                .commissionRate(defaultCommissionRate)
                .commissionAmount(commissionAmount)
                .netAmount(netAmount)
                .bankAccount(request.getBankAccount())
                .bankName(request.getBankName())
                .accountHolder(request.getAccountHolder())
                .reason(request.getReason())
                .build();

        WithdrawalRequestEntity saved = withdrawalRepository.save(withdrawalRequest);
        log.info("Created withdrawal request {} for instructor {}", saved.getId(), instructorId);

        return withdrawalMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WithdrawalRequestResponseDto getWithdrawalRequest(UUID requestId) {
        WithdrawalRequestEntity entity = withdrawalRepository.findByIdAndDeletedFalse(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.withdrawal.notFound")));

        UUID currentUserId = securityUtils.getCurrentUserId();
        if (!securityUtils.isAdmin() && !entity.getInstructorId().equals(currentUserId)) {
            throw new BusinessException(messageProvider.getMessage("exception.withdrawal.accessDenied"));
        }

        return withdrawalMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WithdrawalRequestResponseDto> getInstructorWithdrawals(Pageable pageable) {
        UUID instructorId = securityUtils.getCurrentUserId();
        return withdrawalRepository.findByInstructorIdAndDeletedFalse(instructorId, pageable)
                .map(withdrawalMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WithdrawalRequestResponseDto> getPendingWithdrawals(Pageable pageable) {
        securityUtils.isAdmin();
        return withdrawalRepository.findByStatusAndDeletedFalse(WithdrawalStatusEnum.PENDING, pageable)
                .map(withdrawalMapper::toResponse);
    }

    @Override
    @Transactional
    public WithdrawalRequestResponseDto approveWithdrawal(UUID requestId) {
        securityUtils.isAdmin();

        WithdrawalRequestEntity entity = withdrawalRepository.findByIdAndDeletedFalse(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.withdrawal.notFound")));

        if (entity.getStatus() != WithdrawalStatusEnum.PENDING) {
            throw new BusinessException(messageProvider.getMessage("exception.withdrawal.invalidStatus"));
        }

        entity.setStatus(WithdrawalStatusEnum.APPROVED);
        entity.setApprovedAt(LocalDateTime.now());
        entity.setApprovedBy(securityUtils.getCurrentUserId());

        WithdrawalRequestEntity saved = withdrawalRepository.save(entity);
        log.info("Approved withdrawal request {} for instructor {}", requestId, entity.getInstructorId());

        return withdrawalMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public WithdrawalRequestResponseDto rejectWithdrawal(UUID requestId, String rejectReason) {
        securityUtils.isAdmin();

        WithdrawalRequestEntity entity = withdrawalRepository.findByIdAndDeletedFalse(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.withdrawal.notFound")));

        if (entity.getStatus() != WithdrawalStatusEnum.PENDING) {
            throw new BusinessException(messageProvider.getMessage("exception.withdrawal.invalidStatus"));
        }

        entity.setStatus(WithdrawalStatusEnum.REJECTED);
        entity.setRejectedAt(LocalDateTime.now());
        entity.setRejectReason(rejectReason);

        // Refund the balance back to wallet
        InstructorWalletEntity wallet = walletRepository.findByInstructorIdAndDeletedFalse(entity.getInstructorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.wallet.notFound")));
        wallet.setCurrentBalance(wallet.getCurrentBalance().add(entity.getRequestedAmount()));
        walletRepository.save(wallet);

        WithdrawalRequestEntity saved = withdrawalRepository.save(entity);
        log.info("Rejected withdrawal request {} for instructor {}", requestId, entity.getInstructorId());

        return withdrawalMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public WithdrawalRequestResponseDto completeWithdrawal(UUID requestId, String transactionId) {
        securityUtils.isAdmin();

        WithdrawalRequestEntity entity = withdrawalRepository.findByIdAndDeletedFalse(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.withdrawal.notFound")));

        if (entity.getStatus() != WithdrawalStatusEnum.APPROVED) {
            throw new BusinessException(messageProvider.getMessage("exception.withdrawal.invalidStatus"));
        }

        entity.setStatus(WithdrawalStatusEnum.COMPLETED);
        entity.setCompletedAt(LocalDateTime.now());
        entity.setTransactionId(transactionId);

        // Update wallet with final deductions
        InstructorWalletEntity wallet = walletRepository.findByInstructorIdAndDeletedFalse(entity.getInstructorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.wallet.notFound")));

        wallet.setCurrentBalance(wallet.getCurrentBalance().subtract(entity.getRequestedAmount()));
        wallet.setTotalWithdrawn(wallet.getTotalWithdrawn().add(entity.getNetAmount()));
        wallet.setTotalCommissionDeducted(wallet.getTotalCommissionDeducted().add(entity.getCommissionAmount()));

        walletRepository.save(wallet);
        WithdrawalRequestEntity saved = withdrawalRepository.save(entity);

        log.info("Completed withdrawal request {} for instructor {} with transaction {}",
                 requestId, entity.getInstructorId(), transactionId);

        return withdrawalMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorWalletResponseDto getInstructorWallet() {
        UUID instructorId = securityUtils.getCurrentUserId();
        InstructorWalletEntity wallet = walletRepository.findByInstructorIdAndDeletedFalse(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.wallet.notFound")));

        return withdrawalMapper.toWalletResponse(wallet);
    }

    @Override
    @Transactional
    public WithdrawalRequestResponseDto createRefundWithdrawal(UUID orderId) {
        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(orderId);

        if (orderItems.isEmpty()) {
            throw new ResourceNotFoundException(messageProvider.getMessage("exception.order.itemsNotFound"));
        }

        // Group by instructor and create withdrawal for each
        return orderItems.stream()
                .filter(item -> item.getInstructorId() != null)
                .findFirst()
                .map(firstItem -> {
                    UUID instructorId = firstItem.getInstructorId();
                    BigDecimal totalRefund = orderItems.stream()
                            .filter(item -> instructorId.equals(item.getInstructorId()))
                            .map(OrderItemEntity::getInstructorRevenue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal commissionAmount = calculateCommission(totalRefund, defaultCommissionRate);
                    BigDecimal netAmount = totalRefund.subtract(commissionAmount);

                    WithdrawalRequestEntity refund = WithdrawalRequestEntity.builder()
                            .instructorId(instructorId)
                            .orderId(orderId)
                            .type(WithdrawalTypeEnum.REFUND)
                            .status(WithdrawalStatusEnum.PENDING)
                            .requestedAmount(totalRefund)
                            .commissionRate(defaultCommissionRate)
                            .commissionAmount(commissionAmount)
                            .netAmount(netAmount)
                            .reason("Refund for order: " + orderId)
                            .build();

                    WithdrawalRequestEntity saved = withdrawalRepository.save(refund);
                    log.info("Created refund withdrawal request {} for instructor {} on order {}",
                             saved.getId(), instructorId, orderId);

                    return withdrawalMapper.toResponse(saved);
                })
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.withdrawal.noInstructor")));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WithdrawalRequestResponseDto> getAllWithdrawals(Pageable pageable) {
        securityUtils.isAdmin();
        return withdrawalRepository.findByDeletedFalse(pageable)
                .map(withdrawalMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WithdrawalRequestResponseDto> getWithdrawalsByStatus(WithdrawalStatusEnum status, Pageable pageable) {
        securityUtils.isAdmin();
        return withdrawalRepository.findByStatusAndDeletedFalse(status, pageable)
                .map(withdrawalMapper::toResponse);
    }

    private InstructorWalletEntity createNewWallet(UUID instructorId) {
        InstructorWalletEntity wallet = InstructorWalletEntity.builder()
                .instructorId(instructorId)
                .currentBalance(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO)
                .totalWithdrawn(BigDecimal.ZERO)
                .totalCommissionDeducted(BigDecimal.ZERO)
                .build();
        return walletRepository.save(wallet);
    }

    private BigDecimal calculateCommission(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null || rate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}

