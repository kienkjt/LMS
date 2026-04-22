package com.kjt.lms.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WithdrawalStatusEnum {

    PENDING("1", "Chờ xử lý"),
    APPROVED("2", "Được chấp phát"),
    COMPLETED("3", "Hoàn thành"),
    REJECTED("4", "Bị từ chối"),
    CANCELLED("5", "Bị hủy");

    private final String value;
    private final String description;

    public static WithdrawalStatusEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (WithdrawalStatusEnum type : values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid WithdrawalStatus value: " + value);
    }
}

