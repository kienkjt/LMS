package com.kjt.lms.model.request.notification;

import com.kjt.lms.common.constants.NotificationTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateNotificationRequestDto {

    private UUID userId; // id của người nhận thông báo (cho 1 người)

    private List<UUID> userIds; // danh sách các user nhận thông báo (cho nhiều người)

    @NotNull
    private NotificationTypeEnum type; // loại thông báo (ví dụ: COURSE_UPDATE, NEW_COURSE, etc.)

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String message;
}
