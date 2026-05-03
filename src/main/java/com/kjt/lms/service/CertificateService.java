package com.kjt.lms.service;

import com.kjt.lms.model.entity.EnrollmentEntity;
import com.kjt.lms.model.response.certificate.CertificateResponseDto;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.UUID;

public interface CertificateService {

    CertificateResponseDto issueCertificateIfEligible(EnrollmentEntity enrollment);

    CertificateResponseDto getMyCertificateForCourse(UUID courseId);

    List<CertificateResponseDto> getMyCertificates();

    CertificateResponseDto getCertificate(UUID certificateId);

    Resource loadCertificatePdf(UUID certificateId);
}
