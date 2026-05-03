package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.NotificationTypeEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.model.entity.CertificateEntity;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.entity.EnrollmentEntity;
import com.kjt.lms.model.entity.UserEntity;
import com.kjt.lms.model.response.certificate.CertificateResponseDto;
import com.kjt.lms.repository.CertificateRepository;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.QuizAttemptRepository;
import com.kjt.lms.service.CertificateService;
import com.kjt.lms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateServiceImpl extends BaseService implements CertificateService {

    private static final BigDecimal CERTIFICATE_PROGRESS_THRESHOLD = new BigDecimal("85.00");

    private final CertificateRepository certificateRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final MessageProvider messageProvider;
    private final NotificationService notificationService;

    @Value("${app.certificate.storage-dir:certificates}")
    private String certificateStorageDir;

    @Override
    @Transactional
    public CertificateResponseDto issueCertificateIfEligible(EnrollmentEntity enrollment) {
        if (enrollment.getProgressPercent() == null
                || enrollment.getProgressPercent().compareTo(CERTIFICATE_PROGRESS_THRESHOLD) < 0) {
            return null;
        }

        if (quizAttemptRepository.countUnpassedCourseQuizzes(enrollment.getStudentId(), enrollment.getCourseId()) > 0) {
            return null;
        }

        return certificateRepository.findByEnrollmentIdAndDeletedFalse(enrollment.getId())
                .map(this::toResponse)
                .orElseGet(() -> issueCertificate(enrollment));
    }

    @Override
    @Transactional
    public CertificateResponseDto getMyCertificateForCourse(UUID courseId) {
        UUID userId = securityUtils.getCurrentUserId();
        return certificateRepository.findByUserIdAndCourseIdAndDeletedFalse(userId, courseId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    EnrollmentEntity enrollment = enrollmentRepository.findByStudentIdAndCourseIdAndDeletedFalse(userId, courseId)
                            .orElseThrow(() -> new BusinessException(messageProvider.getMessage("exception.enrollment.required")));
                    CertificateResponseDto certificate = issueCertificateIfEligible(enrollment);
                    if (certificate == null) {
                        throw new ResourceNotFoundException(messageProvider.getMessage("exception.certificate.notFound"));
                    }
                    return certificate;
                });
    }

    @Override
    @Transactional
    public List<CertificateResponseDto> getMyCertificates() {
        UUID userId = securityUtils.getCurrentUserId();
        enrollmentRepository.findByStudentIdAndDeletedFalse(userId)
                .forEach(this::issueCertificateIfEligible);

        return certificateRepository.findByUserIdAndDeletedFalseOrderByIssuedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateResponseDto getCertificate(UUID certificateId) {
        CertificateEntity certificate = getAccessibleCertificate(certificateId);
        return toResponse(certificate);
    }

    @Override
    @Transactional
    public Resource loadCertificatePdf(UUID certificateId) {
        CertificateEntity certificate = getAccessibleCertificate(certificateId);
        Path filePath = certificatePath(certificate);
        if (!Files.exists(filePath)) {
            writeCertificatePdf(certificate);
        }
        return new FileSystemResource(filePath);
    }

    private CertificateResponseDto issueCertificate(EnrollmentEntity enrollment) {
        CourseEntity course = courseRepository.findByIdAndDeletedFalse(enrollment.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.course.notFound")));
        UserEntity student = userRepository.findById(enrollment.getStudentId())
                .filter(user -> !Boolean.TRUE.equals(user.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.user.notfound")));
        UserEntity instructor = userRepository.findById(course.getInstructorId())
                .filter(user -> !Boolean.TRUE.equals(user.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.user.notfound")));

        CertificateEntity certificate = CertificateEntity.builder()
                .userId(student.getId())
                .courseId(course.getId())
                .enrollmentId(enrollment.getId())
                .certificateCode(generateCertificateCode())
                .studentName(student.getFullName())
                .courseTitle(course.getTitle())
                .instructorName(instructor.getFullName())
                .issuedAt(LocalDate.now())
                .build();

        certificate = certificateRepository.save(certificate);
        writeCertificatePdf(certificate);

        certificate.setCertificateUrl("/api/v1/certificates/" + certificate.getId() + "/download");
        certificate = certificateRepository.save(certificate);

        enrollment.setCertificateIssued(true);
        enrollment.setCertificateId(certificate.getId());
        enrollmentRepository.save(enrollment);
        notificationService.notifyUser(
                student.getId(),
                NotificationTypeEnum.CERTIFICATE_ISSUED,
                "Certificate issued",
                "Your certificate for \"" + course.getTitle() + "\" is ready.",
                certificate.getId(),
                "CERTIFICATE"
        );

        log.info("Issued certificate {} for enrollment {}", certificate.getId(), enrollment.getId());
        return toResponse(certificate);
    }

    private CertificateEntity getAccessibleCertificate(UUID certificateId) {
        CertificateEntity certificate = certificateRepository.findByIdAndDeletedFalse(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.certificate.notFound")));

        if (securityUtils.isAdmin() || securityUtils.isCurrentUser(certificate.getUserId())) {
            return certificate;
        }

        CourseEntity course = courseRepository.findByIdAndDeletedFalse(certificate.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.course.notFound")));
        if (securityUtils.isCurrentUser(course.getInstructorId())) {
            return certificate;
        }

        throw new BusinessException(messageProvider.getMessage("exception.certificate.accessDenied"));
    }

    private void writeCertificatePdf(CertificateEntity certificate) {
        try {
            Path directory = Path.of(certificateStorageDir).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            Files.write(certificatePath(certificate), CertificatePdfGenerator.generate(certificate));
        } catch (IOException ex) {
            throw new BusinessException(messageProvider.getMessage("exception.certificate.generateFailed"));
        }
    }

    private Path certificatePath(CertificateEntity certificate) {
        return Path.of(certificateStorageDir)
                .toAbsolutePath()
                .normalize()
                .resolve(certificate.getId() + ".pdf");
    }

    private String generateCertificateCode() {
        return "CERT-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private CertificateResponseDto toResponse(CertificateEntity certificate) {
        return CertificateResponseDto.builder()
                .id(certificate.getId())
                .userId(certificate.getUserId())
                .courseId(certificate.getCourseId())
                .enrollmentId(certificate.getEnrollmentId())
                .certificateCode(certificate.getCertificateCode())
                .studentName(certificate.getStudentName())
                .courseTitle(certificate.getCourseTitle())
                .instructorName(certificate.getInstructorName())
                .certificateUrl(certificate.getCertificateUrl())
                .issuedAt(certificate.getIssuedAt())
                .build();
    }
}
