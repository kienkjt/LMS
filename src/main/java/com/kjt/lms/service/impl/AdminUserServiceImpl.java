package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.constants.NotificationTypeEnum;
import com.kjt.lms.common.constants.WithdrawalStatusEnum;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.RoleEntity;
import com.kjt.lms.model.entity.UserEntity;
import com.kjt.lms.model.entity.EnrollmentEntity;
import com.kjt.lms.model.request.admin.ListUserRequest;
import com.kjt.lms.model.request.admin.UpdateUserStatusRequest;
import com.kjt.lms.model.request.admin.LockUserRequest;
import com.kjt.lms.model.response.admin.UserListResponse;
import com.kjt.lms.model.response.admin.UserDetailResponse;
import com.kjt.lms.repository.RoleRepository;
import com.kjt.lms.repository.UserRepository;
import com.kjt.lms.repository.CourseRepository;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.WithdrawalRequestRepository;
import com.kjt.lms.service.AdminUserService;
import com.kjt.lms.service.EmailService;
import com.kjt.lms.service.NotificationService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserServiceImpl extends BaseService implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final WithdrawalRequestRepository withdrawalRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Override
    @Transactional(readOnly = true)
    public Page<UserListResponse> listUsers(ListUserRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(
                        Sort.Order.desc("updatedAt"),
                        Sort.Order.desc("createdAt")
                )
        );

        Specification<UserEntity> spec = buildUserSpecification(request);
        return userRepository.findAll(spec, pageable)
                .map(this::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.user.notfound")));

        if (user.getDeleted()) {
            throw new ResourceNotFoundException(
                    messageProvider.getMessage("exception.user.notfound"));
        }

        return toDetailResponse(user);
    }

    @Override
    @Transactional
    public UserDetailResponse updateUserStatus(UUID userId, UpdateUserStatusRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.user.notfound")));

        if (user.getDeleted()) {
            throw new ResourceNotFoundException(
                    messageProvider.getMessage("exception.user.notfound"));
        }

        CommonStatusEnum newStatus = request.getActive() ? CommonStatusEnum.ACTIVE : CommonStatusEnum.INACTIVE;
        user.setActive(newStatus);
        user = userRepository.save(user);

        log.info("Updated user {} status to {}", userId, newStatus);
        return toDetailResponse(user);
    }

    @Override
    @Transactional
    public UserDetailResponse lockUser(UUID userId, LockUserRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.user.notfound")));

        if (user.getDeleted()) {
            throw new ResourceNotFoundException(
                    messageProvider.getMessage("exception.user.notfound"));
        }

        // Không cho phép khóa tài khoản của chính mình
        if (securityUtils.isCurrentUser(userId)) {
            throw new BusinessException("Bạn không thể khóa tài khoản của chính mình");
        }

        user.setIsLocked(request.getIsLocked());
        user = userRepository.save(user);

        String action = request.getIsLocked() ? "locked" : "unlocked";

        // Gửi email thông báo khi khóa tài khoản
        if (request.getIsLocked()) {
            emailService.sendAccountLockedEmail(user.getEmail(), user.getFullName(), request.getReason());
            log.info("Sent account locked notification email to user: {}", user.getEmail());
        }

        log.info("User account {} {}", userId, action);
        return toDetailResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.user.notfound")));

        if (user.getDeleted()) {
            throw new ResourceNotFoundException(
                    messageProvider.getMessage("exception.user.notfound"));
        }

        // Không cho phép xóa tài khoản của chính mình
        if (securityUtils.isCurrentUser(userId)) {
            throw new BusinessException("Bạn không thể xóa tài khoản của chính mình");
        }

        // Check if user has active courses (if teacher)
        long courseCount = courseRepository.countByInstructorIdAndDeletedFalse(userId);
        if (courseCount > 0) {
            throw new BusinessException(
                    messageProvider.getMessage("exception.user.hasCourses", courseCount));
        }

        // Check if user has active enrollments (if student)
        List<EnrollmentEntity> enrollments = enrollmentRepository.findByStudentIdAndDeletedFalse(userId);
        if (!enrollments.isEmpty()) {
            throw new BusinessException(
                    messageProvider.getMessage("exception.user.hasEnrollments", enrollments.size()));
        }

        user.setDeleted(true);
        userRepository.save(user);

        log.info("Deleted user {}", userId);
    }

    /**
     * Build Specification for user search and filter
     */
    private Specification<UserEntity> buildUserSpecification(ListUserRequest request) {
        return (root, query, criteriaBuilder) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            // Filter by deleted status
            predicates.add(criteriaBuilder.equal(root.get("deleted"), false));

            // Search by keyword (name, email, phone)
            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                String keyword = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), keyword),
                        criteriaBuilder.like(root.get("phoneNumber"), keyword)
                ));
            }

            // Filter by role - using roleId directly with a subquery
            if (request.getRoleCode() != null && !request.getRoleCode().isEmpty()) {
                RoleEntity roleEntity = roleRepository.findByCode(request.getRoleCode()).orElse(null);
                if (roleEntity != null) {
                    predicates.add(criteriaBuilder.equal(root.get("roleId"), roleEntity.getId()));
                }
            }

            // Filter by active status
            if (request.getActive() != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), request.getActive()));
            }

            // Filter by locked status
            if (request.getIsLocked() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isLocked"), request.getIsLocked()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private UserListResponse toListResponse(UserEntity user) {
        RoleEntity role = roleRepository.findById(user.getRoleId()).orElse(null);
        return UserListResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .roleName(role != null ? role.getName() : "Unknown")
                .roleCode(role != null ? role.getCode() : "UNKNOWN")
                .active(user.getActive())
                .isLocked(user.getIsLocked())
                .isVerified(user.getIsVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private UserDetailResponse toDetailResponse(UserEntity user) {
        RoleEntity role = roleRepository.findById(user.getRoleId()).orElse(null);
        return UserDetailResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .phoneNumber(user.getPhoneNumber())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .roleName(role != null ? role.getName() : "Unknown")
                .roleCode(role != null ? role.getCode() : "UNKNOWN")
                .active(user.getActive())
                .isLocked(user.getIsLocked())
                .isVerified(user.getIsVerified())
                .totalRevenue(user.getTotalRevenue())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

