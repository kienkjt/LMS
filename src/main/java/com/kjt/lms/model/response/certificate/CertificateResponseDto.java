package com.kjt.lms.model.response.certificate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateResponseDto {

    private UUID id;
    private UUID userId;
    private UUID courseId;
    private UUID enrollmentId;
    private String certificateCode;
    private String studentName;
    private String courseTitle;
    private String instructorName;
    private String certificateUrl;
    private LocalDate issuedAt;
}
