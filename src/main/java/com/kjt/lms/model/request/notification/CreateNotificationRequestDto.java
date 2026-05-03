package com.kjt.lms.model.request.notification;

import com.kjt.lms.common.constants.NotificationTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateNotificationRequestDto {

    @NotNull
    private UUID userId;

    @NotNull
    private NotificationTypeEnum type;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String message;

    private UUID referenceId;

    @Size(max = 50)
    private String referenceType;
}
