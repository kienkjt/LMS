package com.kjt.lms.model.request.order;

import com.kjt.lms.common.constants.PaymentMethodEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InitPaymentRequestDto {

    @NotNull(message = "{validation.order.paymentMethod.notBlank}")
    private PaymentMethodEnum paymentMethod;

    private String bankCode;

    private String language;
}

