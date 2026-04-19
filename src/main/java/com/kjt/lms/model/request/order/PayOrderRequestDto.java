package com.kjt.lms.model.request.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PayOrderRequestDto {

    @NotBlank(message = "Transaction ID is required")
    @Size(max = 200, message = "Transaction ID must not exceed 200 characters")
    private String transactionId;
}

