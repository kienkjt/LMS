package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.NotificationTypeEnum;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.NotificationEntity;
import com.kjt.lms.model.request.notification.CreateNotificationRequestDto;
import com.kjt.lms.model.response.notification.NotificationResponseDto;
import com.kjt.lms.repository.NotificationRepository;
import com.kjt.lms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends BaseService implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getMyNotifications(Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();
        return notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return notificationRepository.countByUserIdAndReadFalseAndDeletedFalse(securityUtils.getCurrentUserId());
    }

    @Override
    @Transactional
    public NotificationResponseDto markAsRead(UUID notificationId) {
        UUID userId = securityUtils.getCurrentUserId();
        NotificationEntity notification = notificationRepository.findByIdAndUserIdAndDeletedFalse(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.notification.notFound")));

        if (!Boolean.TRUE.equals(notification.getRead())) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return toResponse(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead() {
        return notificationRepository.markAllAsRead(securityUtils.getCurrentUserId(), LocalDateTime.now());
    }

    @Override
    @Transactional
    public NotificationResponseDto createNotification(CreateNotificationRequestDto request) {
        return toResponse(saveNotification(
                request.getUserId(),
                request.getType(),
                request.getTitle(),
                request.getMessage(),
                request.getReferenceId(),
                request.getReferenceType()
        ));
    }

    @Override
    @Transactional
    public void notifyUser(UUID userId, NotificationTypeEnum type, String title, String message, UUID referenceId, String referenceType) {
        if (userId == null) {
            return;
        }
        saveNotification(userId, type, title, message, referenceId, referenceType);
    }

    private NotificationEntity saveNotification(
            UUID userId,
            NotificationTypeEnum type,
            String title,
            String message,
            UUID referenceId,
            String referenceType) {
        NotificationEntity notification = NotificationEntity.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();
        return notificationRepository.save(notification);
    }

    private NotificationResponseDto toResponse(NotificationEntity notification) {
        return NotificationResponseDto.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(notification.getRead())
                .readAt(notification.getReadAt())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
