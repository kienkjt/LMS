package com.kjt.lms.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class APIResponse<T> {
    private boolean success;
    private int code;
    private String message;
    private T data;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    // Success responses
    public static <T> APIResponse<T> success(T data, String message) {
        return APIResponse.<T>builder()
                .success(true)
                .code(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> APIResponse<T> success(T data) {
        return success(data, "Success");
    }

    public static <T> APIResponse<T> success() {
        return success(null, "Success");
    }

    // Error responses
    public static <T> APIResponse<T> error(String message) {
        return APIResponse.<T>builder()
                .success(false)
                .code(400)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> APIResponse<T> error(int code, String message) {
        return APIResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> APIResponse<T> error(String message, T data) {
        return APIResponse.<T>builder()
                .success(false)
                .code(400)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> APIResponse<T> error(int code, String message, T data) {
        return APIResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}