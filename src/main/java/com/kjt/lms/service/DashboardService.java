package com.kjt.lms.service;

import com.kjt.lms.model.response.dashboard.AdminDashboardResponseDto;
import com.kjt.lms.model.response.dashboard.InstructorDashboardResponseDto;

public interface DashboardService {

    AdminDashboardResponseDto getAdminDashboard();

    InstructorDashboardResponseDto getInstructorDashboard();
}
