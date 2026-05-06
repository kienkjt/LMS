package com.kjt.lms.model.response.learning;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
public class InstructorStudentEngagementDto {
    private UUID enrollmentId;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private String studentAvatar;
    private String studentPhoneNumber;
    private double progressPercent;
    private long currentStreak;
    private long longestStreak;
    private LocalDate lastLearningDate;
    private long inactiveDays;
    private boolean atRisk;
}

