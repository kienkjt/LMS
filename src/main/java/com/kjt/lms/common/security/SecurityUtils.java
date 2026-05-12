package com.kjt.lms.common.security;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.UserEntity;
import com.kjt.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtils {

    private final UserRepository userRepository;
    private final MessageProvider messageProvider;

    /**
     * Lấy UUID của người dùng hiện tại từ SecurityContext
     * @throws ResourceNotFoundException nếu không tìm thấy user hoặc chưa đăng nhập
     */
    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            log.error("Attempt to get current user ID while unauthenticated");
            throw new ResourceNotFoundException(messageProvider.getMessage("exception.auth.unauthenticated"));
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof LmsUserPrincipal lmsUserPrincipal) {
            return lmsUserPrincipal.getId();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(UserEntity::getId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.user.notfoundWithEmail", email)));
    }

    /**
     * Kiểm tra người dùng hiện tại có vai trò ADMIN không
     */
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /**
     * Kiểm tra người dùng hiện tại có phải là chủ sở hữu của một đối tượng không
     */
    public boolean isCurrentUser(UUID userId) {
        try {
            return getCurrentUserId().equals(userId);
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    /**
     * Lấy email người dùng hiện tại (để tương thích với code cũ nếu cần)
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
}
