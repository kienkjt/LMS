package com.kjt.lms.service;

import com.kjt.lms.common.constants.NotificationTypeEnum;
import com.kjt.lms.model.request.notification.CreateNotificationRequestDto;
import com.kjt.lms.model.response.notification.NotificationResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    Page<NotificationResponseDto> getMyNotifications(Pageable pageable);

    long getUnreadCount();

    NotificationResponseDto markAsRead(UUID notificationId);

    int markAllAsRead();

    NotificationResponseDto createNotification(CreateNotificationRequestDto request);

    void notifyUser(UUID userId, NotificationTypeEnum type, String title, String message, UUID referenceId, String referenceType);
}
