package com.kjt.lms.controller;

import com.kjt.lms.common.constants.WithdrawalStatusEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.request.withdrawal.CreateWithdrawalRequestDto;
import com.kjt.lms.model.response.withdrawal.InstructorWalletResponseDto;
import com.kjt.lms.model.response.withdrawal.WithdrawalRequestResponseDto;
import com.kjt.lms.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/withdrawal")
@RequiredArgsConstructor
@Tag(name = "Withdrawal Management", description = "Endpoints for managing instructor withdrawals and refunds")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;
    private final MessageProvider messageProvider;

    @PostMapping("/request")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Create withdrawal request", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<WithdrawalRequestResponseDto>> createWithdrawalRequest(
            @Valid @RequestBody CreateWithdrawalRequestDto request) {
        WithdrawalRequestResponseDto response = withdrawalService.createWithdrawalRequest(request);
        return ResponseEntity.ok(APIResponse.success(response,
                messageProvider.getMessage("withdrawal.request.created.success")));
    }

    @GetMapping("/request/{requestId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get withdrawal request details", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<WithdrawalRequestResponseDto>> getWithdrawalRequest(
            @PathVariable UUID requestId) {
        WithdrawalRequestResponseDto response = withdrawalService.getWithdrawalRequest(requestId);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @GetMapping("/requests")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Get my withdrawal requests", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Page<WithdrawalRequestResponseDto>>> getMyWithdrawals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WithdrawalRequestResponseDto> response = withdrawalService.getInstructorWithdrawals(pageable);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @GetMapping("/wallet")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Get my wallet information", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<InstructorWalletResponseDto>> getWallet() {
        InstructorWalletResponseDto response = withdrawalService.getInstructorWallet();
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pending withdrawal requests (admin only)", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Page<WithdrawalRequestResponseDto>>> getPendingWithdrawals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WithdrawalRequestResponseDto> response = withdrawalService.getPendingWithdrawals(pageable);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all withdrawal requests (admin only)", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Page<WithdrawalRequestResponseDto>>> getAllWithdrawals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WithdrawalRequestResponseDto> response = withdrawalService.getAllWithdrawals(pageable);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @GetMapping("/admin/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get withdrawal requests by status (admin only)", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Page<WithdrawalRequestResponseDto>>> getWithdrawalsByStatus(
            @PathVariable WithdrawalStatusEnum status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WithdrawalRequestResponseDto> response = withdrawalService.getWithdrawalsByStatus(status, pageable);
        return ResponseEntity.ok(APIResponse.success(response, null));
    }

    @PostMapping("/admin/approve/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve withdrawal request (admin only)", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<WithdrawalRequestResponseDto>> approveWithdrawal(
            @PathVariable UUID requestId) {
        WithdrawalRequestResponseDto response = withdrawalService.approveWithdrawal(requestId);
        return ResponseEntity.ok(APIResponse.success(response,
                messageProvider.getMessage("withdrawal.request.approved.success")));
    }

    @PostMapping("/admin/reject/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject withdrawal request (admin only)", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<WithdrawalRequestResponseDto>> rejectWithdrawal(
            @PathVariable UUID requestId,
            @RequestParam String rejectReason) {
        WithdrawalRequestResponseDto response = withdrawalService.rejectWithdrawal(requestId, rejectReason);
        return ResponseEntity.ok(APIResponse.success(response,
                messageProvider.getMessage("withdrawal.request.rejected.success")));
    }

    @PostMapping("/admin/complete/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Complete withdrawal request (admin only)", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<WithdrawalRequestResponseDto>> completeWithdrawal(
            @PathVariable UUID requestId,
            @RequestParam String transactionId) {
        WithdrawalRequestResponseDto response = withdrawalService.completeWithdrawal(requestId, transactionId);
        return ResponseEntity.ok(APIResponse.success(response,
                messageProvider.getMessage("withdrawal.request.completed.success")));
    }

    @PostMapping("/request/{requestId}/cancel")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Cancel my pending withdrawal request", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<WithdrawalRequestResponseDto>> cancelWithdrawalRequest(
            @PathVariable UUID requestId) {
        WithdrawalRequestResponseDto response = withdrawalService.cancelWithdrawalRequest(requestId);
        return ResponseEntity.ok(APIResponse.success(response,
                messageProvider.getMessage("withdrawal.request.cancelled.success")));
    }
}

