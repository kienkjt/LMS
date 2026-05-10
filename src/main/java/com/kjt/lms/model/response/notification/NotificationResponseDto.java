package com.kjt.lms.model.response.notification;

import com.kjt.lms.common.constants.NotificationTypeEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class NotificationResponseDto {

    private UUID id;
    private UUID userId;
    private NotificationTypeEnum type;
    private String title;
    private String message;
    private Boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
