package com.kjt.lms.service.impl;

import com.kjt.lms.common.base.BaseService;
import com.kjt.lms.common.constants.NotificationTypeEnum;
import com.kjt.lms.model.entity.PlatformSettingEntity;
import com.kjt.lms.model.entity.UserEntity;
import com.kjt.lms.model.response.admin.PlatformFeeResponseDto;
import com.kjt.lms.repository.PlatformSettingRepository;
import com.kjt.lms.repository.UserRepository;
import com.kjt.lms.service.EmailService;
import com.kjt.lms.service.NotificationService;
import com.kjt.lms.service.PlatformFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlatformFeeServiceImpl extends BaseService implements PlatformFeeService {

    private static final String PLATFORM_FEE_KEY = "PLATFORM_FEE_PERCENT";
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Value("${app.platform.fee-percent:20}")
    private BigDecimal defaultPlatformFeePercent;

    private final PlatformSettingRepository platformSettingRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Override
    public BigDecimal getInstructorRevenueRate() {
        BigDecimal platformFeePercent = getCurrentPlatformFeePercent();
        return BigDecimal.ONE.subtract(platformFeePercent.divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP));
    }

    @Override
    public PlatformFeeResponseDto getPlatformFee() {
        BigDecimal platformFeePercent = getCurrentPlatformFeePercent();
        BigDecimal instructorRevenuePercent = ONE_HUNDRED.subtract(platformFeePercent);
        return PlatformFeeResponseDto.builder()
                .platformFeePercent(platformFeePercent)
                .instructorRevenuePercent(instructorRevenuePercent)
                .build();
    }

    @Override
    @Transactional
    public PlatformFeeResponseDto updatePlatformFee(BigDecimal platformFeePercent) {
        BigDecimal normalizedPercent = platformFeePercent.setScale(2, RoundingMode.HALF_UP);
        PlatformSettingEntity setting = platformSettingRepository.findBySettingKeyAndDeletedFalse(PLATFORM_FEE_KEY)
                .orElseGet(() -> PlatformSettingEntity.builder()
                        .settingKey(PLATFORM_FEE_KEY)
                        .description("Platform fee percent for course sales")
                        .build());
        setting.setSettingValue(normalizedPercent.toPlainString());
        platformSettingRepository.save(setting);

        notifyInstructors(normalizedPercent);
        return getPlatformFee();
    }

    private BigDecimal getCurrentPlatformFeePercent() {
        return platformSettingRepository.findBySettingKeyAndDeletedFalse(PLATFORM_FEE_KEY)
                .map(PlatformSettingEntity::getSettingValue)
                .map(BigDecimal::new)
                .orElse(defaultPlatformFeePercent)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void notifyInstructors(BigDecimal platformFeePercent) {
        BigDecimal instructorPercent = ONE_HUNDRED.subtract(platformFeePercent).setScale(2, RoundingMode.HALF_UP);
        String title = "Cập nhật phí nền tảng";
        String message = String.format(
                "Phí nền tảng đã cập nhật thành %s%%. Tỷ lệ doanh thu của giảng viên hiện là %s%% cho các đơn hàng mới.",
                platformFeePercent.toPlainString(),
                instructorPercent.toPlainString()
        );

        List<UserEntity> instructors = userRepository.findAllInstructors();
        for (UserEntity instructor : instructors) {
            notificationService.notifyUser(
                    instructor.getId(),
                    NotificationTypeEnum.SYSTEM,
                    title,
                    message,
                    null,
                    "PLATFORM_FEE"
            );
            if (instructor.getEmail() != null && !instructor.getEmail().isBlank()) {
                emailService.sendSystemNotificationEmail(
                        instructor.getEmail(),
                        instructor.getFullName(),
                        title,
                        message
                );
            }
        }
    }
}
