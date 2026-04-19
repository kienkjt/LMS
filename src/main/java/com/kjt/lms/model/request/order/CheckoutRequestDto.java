package com.kjt.lms.model.request.order;

import com.kjt.lms.common.constants.PaymentMethodEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutRequestDto {
    @NotNull(message = "Payment method is required")
    private PaymentMethodEnum paymentMethod;
    private String note;
}
