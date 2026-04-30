package com.kjt.lms.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WithdrawalTypeEnum {

    REFUND("1", "Hoan tien"),
    EARNINGS("2", "Rut tien kiem duoc"),
    SETTLEMENT("3", "Doi soat doanh thu cho giai ngan");

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
