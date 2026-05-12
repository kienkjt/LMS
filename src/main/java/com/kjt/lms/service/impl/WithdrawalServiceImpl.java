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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalServiceImpl extends BaseService implements WithdrawalService {

    private static final EnumSet<WithdrawalStatusEnum> RESERVED_WITHDRAWAL_STATUSES =
            EnumSet.of(WithdrawalStatusEnum.PENDING, WithdrawalStatusEnum.APPROVED);

    @Value("${app.withdrawal.commission-rate:10}")
    private BigDecimal defaultCommissionRate;

    @Value("${app.withdrawal.settlement-delay-minutes:10080}")
    private long settlementDelayMinutes;

    private final InstructorWalletRepository walletRepository;
    private final WithdrawalRequestRepository withdrawalRepository;
    private final OrderItemRepository orderItemRepository;
    private final WithdrawalMapper withdrawalMapper;
    private final SecurityUtils securityUtils;
    private final MessageProvider messageProvider;

    @Override
    @Transactional
    public void addEarnings(UUID instructorId, UUID orderId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || orderId == null) {
            return;
        }

        InstructorWalletEntity wallet = getOrCreateWallet(instructorId);
        LocalDateTime now = LocalDateTime.now();

        // Tính phí ngay lập tức khi thanh toán (Earn first → Fee deducted immediately)
        BigDecimal commissionAmount = calculateCommission(amount, defaultCommissionRate);
        BigDecimal netAmount = amount.subtract(commissionAmount);

        // Thêm net amount (đã trừ phí) vào pendingBalance
        wallet.setPendingBalance(wallet.getPendingBalance().add(netAmount));
        wallet.setTotalEarned(wallet.getTotalEarned().add(netAmount));
        wallet.setTotalCommissionDeducted(wallet.getTotalCommissionDeducted().add(commissionAmount));
        walletRepository.save(wallet);

        WithdrawalRequestEntity settlement = WithdrawalRequestEntity.builder()
                .instructorId(instructorId)
                .orderId(orderId)
                .type(WithdrawalTypeEnum.SETTLEMENT)
                .status(WithdrawalStatusEnum.PENDING)
                .requestedAmount(netAmount)
                .commissionRate(defaultCommissionRate)
                .commissionAmount(commissionAmount)
                .netAmount(netAmount)
                .reason("Pending settlement for order: " + orderId)
                .availableAt(now.plusMinutes(settlementDelayMinutes))
                .build();
        withdrawalRepository.save(settlement);

        log.info("Added pending earnings {} (after commission {}) for instructor {} and order {}",
                netAmount, commissionAmount, instructorId, orderId);
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

        // Phí đã được trừ ở addEarnings, rút tiền không trừ phí thêm
        // Commission is already deducted at payment time
        BigDecimal netAmount = request.getRequestedAmount();

        wallet.setCurrentBalance(wallet.getCurrentBalance().subtract(request.getRequestedAmount()));
        walletRepository.save(wallet);

        WithdrawalRequestEntity withdrawalRequest = WithdrawalRequestEntity.builder()
                .instructorId(instructorId)
                .type(WithdrawalTypeEnum.EARNINGS)
                .status(WithdrawalStatusEnum.PENDING)
                .requestedAmount(request.getRequestedAmount())
                .commissionRate(BigDecimal.ZERO)
                .commissionAmount(BigDecimal.ZERO)
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
        return withdrawalRepository.findByTypeAndStatusAndDeletedFalse(
                        WithdrawalTypeEnum.EARNINGS,
                        WithdrawalStatusEnum.PENDING,
                        pageable
                )
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

        releaseReservedFundsIfNeeded(entity);

        WithdrawalRequestEntity saved = withdrawalRepository.save(entity);
        log.info("Rejected withdrawal request {} for instructor {}", requestId, entity.getInstructorId());

        return withdrawalMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public WithdrawalRequestResponseDto cancelWithdrawalRequest(UUID requestId) {
        UUID instructorId = securityUtils.getCurrentUserId();
        WithdrawalRequestEntity entity = withdrawalRepository.findByIdAndDeletedFalse(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.withdrawal.notFound")));

        if (!entity.getInstructorId().equals(instructorId)) {
            throw new BusinessException(messageProvider.getMessage("exception.withdrawal.accessDenied"));
        }

        if (entity.getStatus() != WithdrawalStatusEnum.PENDING) {
            throw new BusinessException(messageProvider.getMessage("exception.withdrawal.invalidStatus"));
        }
        if (entity.getType() != WithdrawalTypeEnum.EARNINGS) {
            throw new BusinessException(messageProvider.getMessage("exception.withdrawal.invalidStatus"));
        }

        entity.setStatus(WithdrawalStatusEnum.CANCELLED);
        entity.setRejectedAt(LocalDateTime.now());
        entity.setRejectReason("Cancelled by instructor");
        releaseReservedFundsIfNeeded(entity);

        WithdrawalRequestEntity saved = withdrawalRepository.save(entity);
        log.info("Cancelled withdrawal request {} for instructor {}", requestId, instructorId);
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

        if (entity.getType() == WithdrawalTypeEnum.EARNINGS) {
            InstructorWalletEntity wallet = walletRepository.findByInstructorIdAndDeletedFalse(entity.getInstructorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            messageProvider.getMessage("exception.wallet.notFound")));
            // Commission đã trừ ở addEarnings, chỉ cập nhật totalWithdrawn
            wallet.setTotalWithdrawn(wallet.getTotalWithdrawn().add(zeroIfNull(entity.getNetAmount())));
            walletRepository.save(wallet);
        }

        WithdrawalRequestEntity saved = withdrawalRepository.save(entity);

        log.info("Completed withdrawal request {} for instructor {} with transaction {}",
                requestId, entity.getInstructorId(), transactionId);

        return withdrawalMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorWalletResponseDto getInstructorWallet() {
        UUID instructorId = securityUtils.getCurrentUserId();
        InstructorWalletEntity wallet = getOrCreateWallet(instructorId);
        BigDecimal pendingWithdrawalAmount = getPendingWithdrawalAmount(instructorId);

        InstructorWalletResponseDto response = withdrawalMapper.toWalletResponse(wallet);
        response.setPendingBalance(wallet.getPendingBalance());
        response.setAvailableBalance(wallet.getCurrentBalance());
        response.setCurrentBalance(wallet.getCurrentBalance());
        response.setTotalBalance(wallet.getCurrentBalance().add(wallet.getPendingBalance()));
        response.setPendingWithdrawalAmount(pendingWithdrawalAmount);
        return response;
    }

    @Override
    @Transactional
    public void processRefundAdjustment(UUID orderId) {
        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(orderId);

        if (orderItems.isEmpty()) {
            throw new ResourceNotFoundException(messageProvider.getMessage("exception.order.itemsNotFound"));
        }

        Map<UUID, BigDecimal> refundByInstructor = orderItems.stream()
                .filter(item -> item.getInstructorId() != null)
                .collect(Collectors.groupingBy(
                        OrderItemEntity::getInstructorId,
                        Collectors.mapping(
                                item -> zeroIfNull(item.getInstructorRevenue()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        if (refundByInstructor.isEmpty()) {
            throw new ResourceNotFoundException(messageProvider.getMessage("exception.withdrawal.noInstructor"));
        }

        for (Map.Entry<UUID, BigDecimal> entry : refundByInstructor.entrySet()) {
            createRefundLedgerEntry(orderId, entry.getKey(), entry.getValue());
        }
    }

    @Override
    @Transactional
    @Scheduled(cron = "${app.withdrawal.settlement-release-cron:0 0 * * * *}")
    public int releasePendingEarnings() {
        List<WithdrawalRequestEntity> settlements =
                withdrawalRepository.findByTypeAndStatusAndAvailableAtLessThanEqualAndDeletedFalse(
                        WithdrawalTypeEnum.SETTLEMENT,
                        WithdrawalStatusEnum.PENDING,
                        LocalDateTime.now()
                );

        int releasedCount = 0;
        for (WithdrawalRequestEntity settlement : settlements) {
            InstructorWalletEntity wallet = getOrCreateWallet(settlement.getInstructorId());
            BigDecimal amount = zeroIfNull(settlement.getRequestedAmount());

            if (wallet.getPendingBalance().compareTo(amount) < 0) {
                throw new BusinessException(messageProvider.getMessage("exception.wallet.inconsistentLedger"));
            }

            wallet.setPendingBalance(wallet.getPendingBalance().subtract(amount));
            wallet.setCurrentBalance(wallet.getCurrentBalance().add(amount));
            walletRepository.save(wallet);

            settlement.setStatus(WithdrawalStatusEnum.COMPLETED);
            settlement.setCompletedAt(LocalDateTime.now());
            settlement.setNetAmount(amount);
            withdrawalRepository.save(settlement);
            releasedCount++;
        }

        if (releasedCount > 0) {
            log.info("Released {} pending settlement entries", releasedCount);
        }
        return releasedCount;
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
                .pendingBalance(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO)
                .totalWithdrawn(BigDecimal.ZERO)
                .totalCommissionDeducted(BigDecimal.ZERO)
                .totalRefunded(BigDecimal.ZERO)
                .build();
        return walletRepository.save(wallet);
    }

    private InstructorWalletEntity getOrCreateWallet(UUID instructorId) {
        return walletRepository.findByInstructorIdAndDeletedFalse(instructorId)
                .orElseGet(() -> createNewWallet(instructorId));
    }

    private void releaseReservedFundsIfNeeded(WithdrawalRequestEntity entity) {
        if (entity.getType() != WithdrawalTypeEnum.EARNINGS) {
            return;
        }

        InstructorWalletEntity wallet = walletRepository.findByInstructorIdAndDeletedFalse(entity.getInstructorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.wallet.notFound")));
        wallet.setCurrentBalance(wallet.getCurrentBalance().add(entity.getRequestedAmount()));
        walletRepository.save(wallet);
    }

    private BigDecimal getPendingWithdrawalAmount(UUID instructorId) {
        return zeroIfNull(withdrawalRepository.sumRequestedAmountByInstructorIdAndTypeAndStatusInAndDeletedFalse(
                instructorId,
                WithdrawalTypeEnum.EARNINGS,
                RESERVED_WITHDRAWAL_STATUSES
        ));
    }

    private void createRefundLedgerEntry(UUID orderId, UUID instructorId, BigDecimal refundAmount) {
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (withdrawalRepository.existsByOrderIdAndInstructorIdAndTypeAndDeletedFalse(
                orderId, instructorId, WithdrawalTypeEnum.REFUND)) {
            throw new BusinessException(messageProvider.getMessage("exception.refund.alreadyProcessed"));
        }

        InstructorWalletEntity wallet = getOrCreateWallet(instructorId);

        BigDecimal pendingToDeduct = refundAmount.min(wallet.getPendingBalance());
        BigDecimal remainingRefund = refundAmount.subtract(pendingToDeduct);

        if (wallet.getCurrentBalance().compareTo(remainingRefund) < 0) {
            throw new BusinessException(messageProvider.getMessage("exception.refund.walletInsufficientBalance"));
        }

        if (wallet.getTotalEarned().compareTo(refundAmount) < 0) {
            throw new BusinessException(messageProvider.getMessage("exception.wallet.inconsistentLedger"));
        }

        wallet.setPendingBalance(wallet.getPendingBalance().subtract(pendingToDeduct));
        wallet.setCurrentBalance(wallet.getCurrentBalance().subtract(remainingRefund));
        wallet.setTotalEarned(wallet.getTotalEarned().subtract(refundAmount));
        wallet.setTotalRefunded(wallet.getTotalRefunded().add(refundAmount));
        walletRepository.save(wallet);

        settlePendingLedgerForRefund(orderId, instructorId, pendingToDeduct);

        WithdrawalRequestEntity refund = WithdrawalRequestEntity.builder()
                .instructorId(instructorId)
                .orderId(orderId)
                .type(WithdrawalTypeEnum.REFUND)
                .status(WithdrawalStatusEnum.COMPLETED)
                .requestedAmount(refundAmount)
                .commissionRate(BigDecimal.ZERO)
                .commissionAmount(BigDecimal.ZERO)
                .netAmount(refundAmount)
                .reason("Refund adjustment for order: " + orderId)
                .approvedAt(LocalDateTime.now())
                .approvedBy(securityUtils.getCurrentUserId())
                .completedAt(LocalDateTime.now())
                .transactionId("REFUND-" + orderId.toString().replace("-", "").substring(0, 12)
                        + "-" + instructorId.toString().replace("-", "").substring(0, 6))
                .build();
        withdrawalRepository.save(refund);
    }

    private void settlePendingLedgerForRefund(UUID orderId, UUID instructorId, BigDecimal pendingDeducted) {
        if (pendingDeducted.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        List<WithdrawalRequestEntity> settlements =
                withdrawalRepository.findByInstructorIdAndOrderIdAndTypeAndStatusAndDeletedFalse(
                        instructorId,
                        orderId,
                        WithdrawalTypeEnum.SETTLEMENT,
                        WithdrawalStatusEnum.PENDING
                );

        BigDecimal remaining = pendingDeducted;
        for (WithdrawalRequestEntity settlement : settlements) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal settlementAmount = zeroIfNull(settlement.getRequestedAmount());
            if (settlementAmount.compareTo(remaining) > 0) {
                settlement.setRequestedAmount(settlementAmount.subtract(remaining));
                settlement.setNetAmount(settlementAmount.subtract(remaining));
                withdrawalRepository.save(settlement);
                remaining = BigDecimal.ZERO;
                break;
            }

            settlement.setRequestedAmount(BigDecimal.ZERO);
            settlement.setNetAmount(BigDecimal.ZERO);
            settlement.setStatus(WithdrawalStatusEnum.CANCELLED);
            settlement.setRejectedAt(LocalDateTime.now());
            settlement.setRejectReason("Cancelled by refund for order: " + orderId);
            withdrawalRepository.save(settlement);
            remaining = remaining.subtract(settlementAmount);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(messageProvider.getMessage("exception.wallet.inconsistentLedger"));
        }
    }

    private BigDecimal calculateCommission(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null || rate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(rate).divide(BigDecimal.valueOf(100), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
