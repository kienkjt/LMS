package com.kjt.lms.model.request.order;

import com.kjt.lms.common.constants.PaymentMethodEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PayOrderRequestDto {

    @NotNull(message = "{validation.order.paymentMethod.notBlank}")
    private PaymentMethodEnum paymentMethod;

    @NotBlank(message = "{validation.payment.transactionId.notBlank}")
    @Size(max = 200, message = "{validation.payment.transactionId.size}")
    private String transactionId;
}
