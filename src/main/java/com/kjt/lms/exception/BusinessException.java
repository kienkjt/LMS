package com.kjt.lms.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private Integer errorCode;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Integer errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}