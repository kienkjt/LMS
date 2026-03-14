package com.kjt.lms.model.entity;

import com.kjt.lms.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "certificates")
public class CertificateEntity extends BaseEntity {

    @Column(name = "userId", nullable = false)
    private UUID userId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "certificate_code", nullable = false, unique = true, length = 100)
    private String certificateCode;

    @Column(name = "student_name", nullable = false, length = 100)
    private String studentName;

    @Column(name = "course_title", nullable = false, length = 200)
    private String courseTitle;

    @Column(name = "instructor_name", length = 100)
    private String instructorName;

    @Column(name = "certificate_url", length = 500)
    private String certificateUrl;

    @Column(name = "issused_at")
    private LocalDate issuedAt;
}
