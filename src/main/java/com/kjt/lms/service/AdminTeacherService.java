package com.kjt.lms.service;

import com.kjt.lms.model.request.admin.ListUserRequest;
import com.kjt.lms.model.response.admin.UserListResponse;
import com.kjt.lms.model.response.admin.UserDetailResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.UUID;

public interface AdminTeacherService {
    /**
     * Lấy danh sách giáo viên
     */
    Page<UserListResponse> listTeachers(ListUserRequest request);

    /**
     * Lấy chi tiết một giáo viên
     */
    UserDetailResponse getTeacherDetail(UUID teacherId);

    /**
     * Lấy số lượng khóa học của giáo viên
     */
    long getTeacherCourseCount(UUID teacherId);

    /**
     * Lấy số lượng học viên của giáo viên (tính từ enrollment)
     */
    long getTeacherStudentCount(UUID teacherId);

    /**
     * Lấy tổng doanh thu của giáo viên
     */
   BigDecimal getTeacherTotalRevenue(UUID teacherId);
}

