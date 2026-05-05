package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.RoleEntity;
import com.kjt.lms.model.entity.UserEntity;
import com.kjt.lms.model.request.admin.ListUserRequest;
import com.kjt.lms.model.response.admin.UserListResponse;
import com.kjt.lms.model.response.admin.UserDetailResponse;
import com.kjt.lms.repository.CourseRepository;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.RoleRepository;
import com.kjt.lms.repository.UserRepository;
import com.kjt.lms.service.AdminTeacherService;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminTeacherServiceImpl extends BaseService implements AdminTeacherService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserListResponse> listTeachers(ListUserRequest request) {
        // Set role filter to TEACHER
        ListUserRequest teacherRequest = ListUserRequest.builder()
                .keyword(request.getKeyword())
                .roleCode("TEACHER")
                .active(request.getActive())
                .isLocked(request.getIsLocked())
                .page(request.getPage())
                .size(request.getSize())
                .build();

        Pageable pageable = PageRequest.of(
                teacherRequest.getPage(),
                teacherRequest.getSize(),
                Sort.by(
                        Sort.Order.desc("updatedAt"),
                        Sort.Order.desc("createdAt")
                )
        );

        Specification<UserEntity> spec = buildTeacherSpecification(teacherRequest);
        return userRepository.findAll(spec, pageable)
                .map(this::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResponse getTeacherDetail(UUID teacherId) {
        UserEntity teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.user.notfound")));

        if (teacher.getDeleted()) {
            throw new ResourceNotFoundException(
                    messageProvider.getMessage("exception.user.notfound"));
        }

        // Verify user is a teacher
        RoleEntity role = roleRepository.findById(teacher.getRoleId())
                .orElseThrow(() -> new BusinessException("Vai trò không tìm thấy"));

        if (!"TEACHER".equals(role.getCode())) {
            throw new BusinessException("Người dùng này không phải là giáo viên");
        }

        return toDetailResponse(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTeacherCourseCount(UUID teacherId) {
        return courseRepository.countByInstructorIdAndDeletedFalse(teacherId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTeacherStudentCount(UUID teacherId) {
        return enrollmentRepository.countByInstructorId(teacherId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTeacherTotalRevenue(UUID teacherId) {
        UserEntity teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.user.notfound")));
        return teacher.getTotalRevenue() != null ? teacher.getTotalRevenue() : BigDecimal.ZERO;
    }

    /**
     * Build Specification for teacher search and filter
     */
    private Specification<UserEntity> buildTeacherSpecification(ListUserRequest request) {
        return (root, query, criteriaBuilder) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();

            // Filter by deleted status
            predicates.add(criteriaBuilder.equal(root.get("deleted"), false));

            // Filter by TEACHER role
            if ("INSTRUCTOR".equals(request.getRoleCode())) {
                var roleJoin = root.join("roleId");
                predicates.add(criteriaBuilder.equal(roleJoin.get("code"), "INSTRUCTOR"));
            }

            // Search by keyword (name, email, phone)
            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                String keyword = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), keyword),
                        criteriaBuilder.like(root.get("phoneNumber"), keyword)
                ));
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

