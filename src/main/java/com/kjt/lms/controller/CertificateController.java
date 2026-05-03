package com.kjt.lms.controller;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.response.certificate.CertificateResponseDto;
import com.kjt.lms.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
@Tag(name = "Certificates", description = "Course completion certificates")
public class CertificateController {

    private final CertificateService certificateService;
    private final MessageProvider messageProvider;

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get current user's certificates", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<List<CertificateResponseDto>>> getMyCertificates() {
        List<CertificateResponseDto> response = certificateService.getMyCertificates();
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("certificate.list.success")));
    }

    @GetMapping("/courses/{courseId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get current user's certificate for a course", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<CertificateResponseDto>> getMyCertificateForCourse(@PathVariable UUID courseId) {
        CertificateResponseDto response = certificateService.getMyCertificateForCourse(courseId);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("certificate.detail.success")));
    }

    @GetMapping("/{certificateId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get certificate details", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<APIResponse<CertificateResponseDto>> getCertificate(@PathVariable UUID certificateId) {
        CertificateResponseDto response = certificateService.getCertificate(certificateId);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("certificate.detail.success")));
    }

    @GetMapping("/{certificateId}/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Download certificate PDF", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<Resource> downloadCertificate(@PathVariable UUID certificateId) {
        Resource resource = certificateService.loadCertificatePdf(certificateId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("certificate-" + certificateId + ".pdf")
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}
