package com.kjt.lms.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WithdrawalTypeEnum {

    REFUND("1", "Hoàn tiền"),
    EARNINGS("2", "Rút tiền kiếm được");

    private final String value;
    private final String description;

    public static WithdrawalTypeEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (WithdrawalTypeEnum type : values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid WithdrawalType value: " + value);
    }
}

