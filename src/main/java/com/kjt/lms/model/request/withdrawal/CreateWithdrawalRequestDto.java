package com.kjt.lms.model.request.withdrawal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CreateWithdrawalRequestDto {
    private String reason;
    @Size(max = 500, message = "{validation.withdrawalRequest.reason.size}")

    private String accountHolder;
    @Size(min = 2, max = 100, message = "{validation.withdrawalRequest.accountHolder.size}")
    @NotBlank(message = "{validation.withdrawalRequest.accountHolder.notBlank}")

    private String bankName;
    @Size(min = 2, max = 100, message = "{validation.withdrawalRequest.bankName.size}")
    @NotBlank(message = "{validation.withdrawalRequest.bankName.notBlank}")

    private String bankAccount;
    @Size(min = 8, max = 50, message = "{validation.withdrawalRequest.bankAccount.size}")
    @NotBlank(message = "{validation.withdrawalRequest.bankAccount.notBlank}")

    @DecimalMin(value = "0.01", message = "{validation.withdrawalRequest.requestedAmount.min}")
    @NotNull(message = "{validation.withdrawalRequest.requestedAmount.notNull}")
    private BigDecimal requestedAmount;

}
