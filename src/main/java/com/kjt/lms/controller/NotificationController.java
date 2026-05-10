package com.kjt.lms.controller;

import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.request.notification.CreateNotificationRequestDto;
import com.kjt.lms.model.response.notification.NotificationResponseDto;
import com.kjt.lms.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notification inbox")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get current user's notifications", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Page<NotificationResponseDto>>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return ResponseEntity.ok(APIResponse.success(notificationService.getMyNotifications(pageable), null));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get unread notification count", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Map<String, Long>>> getUnreadCount() {
        return ResponseEntity.ok(APIResponse.success(Map.of("count", notificationService.getUnreadCount()), null));
    }

    @PostMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Mark notification as read", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<NotificationResponseDto>> markAsRead(@PathVariable UUID notificationId) {
        return ResponseEntity.ok(APIResponse.success(notificationService.markAsRead(notificationId), null));
    }

    @PostMapping("/read-all")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Mark all current user's notifications as read", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Map<String, Integer>>> markAllAsRead() {
        return ResponseEntity.ok(APIResponse.success(Map.of("updated", notificationService.markAllAsRead()), null));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create notification(s) - Send to single user (userId) or multiple users (userIds). Admin only", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<Object>> createNotification(
            @Valid @RequestBody CreateNotificationRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.success(notificationService.createNotification(request), null));
    }
}
