package com.kjt.lms.service;

import com.kjt.lms.model.response.dashboard.AdminReportResponseDto;
import com.kjt.lms.model.response.dashboard.InstructorReportResponseDto;

public interface ReportService {
    AdminReportResponseDto getAdminReport(Integer year, Integer month, Integer days);

    InstructorReportResponseDto getInstructorReport(Integer year, Integer month, Integer days);
}
