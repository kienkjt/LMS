package com.kjt.lms.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentMethodEnum {

    VNPAY("1", "Thanh toán VNPAY"),
    BANK_TRANSFER("2", "Chuyển khoản ngân hàng"),
    FREE("3", "Miễn phí");

    private final String value;
    private final String description;

    public static PaymentMethodEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (PaymentMethodEnum type : values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid PaymentMethod value: " + value);
    }
}