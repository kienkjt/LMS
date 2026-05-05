package com.kjt.lms.repository;

import com.kjt.lms.model.entity.PlatformSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlatformSettingRepository extends JpaRepository<PlatformSettingEntity, UUID> {
    Optional<PlatformSettingEntity> findBySettingKeyAndDeletedFalse(String settingKey);
}
