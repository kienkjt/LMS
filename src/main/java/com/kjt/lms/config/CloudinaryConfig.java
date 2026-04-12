package com.kjt.lms.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@Slf4j
public class CloudinaryConfig {

    @Value("${cloudinary.accounts.avatar.cloud-name:${cloudinary.cloud-name:}}")
    private String avatarCloudName;

    @Value("${cloudinary.accounts.avatar.api-key:${cloudinary.api-key:}}")
    private String avatarApiKey;

    @Value("${cloudinary.accounts.avatar.api-secret:${cloudinary.api-secret:}}")
    private String avatarApiSecret;

    @Value("${cloudinary.accounts.course-image.cloud-name:${cloudinary.accounts.avatar.cloud-name:${cloudinary.cloud-name:}}}")
    private String courseImageCloudName;

    @Value("${cloudinary.accounts.course-image.api-key:${cloudinary.accounts.avatar.api-key:${cloudinary.api-key:}}}")
    private String courseImageApiKey;

    @Value("${cloudinary.accounts.course-image.api-secret:${cloudinary.accounts.avatar.api-secret:${cloudinary.api-secret:}}}")
    private String courseImageApiSecret;

    @Value("${cloudinary.accounts.course-video.cloud-name:${cloudinary.accounts.avatar.cloud-name:${cloudinary.cloud-name:}}}")
    private String courseVideoCloudName;

    @Value("${cloudinary.accounts.course-video.api-key:${cloudinary.accounts.avatar.api-key:${cloudinary.api-key:}}}")
    private String courseVideoApiKey;

    @Value("${cloudinary.accounts.course-video.api-secret:${cloudinary.accounts.avatar.api-secret:${cloudinary.api-secret:}}}")
    private String courseVideoApiSecret;

    @Bean("avatarCloudinary")
    @Primary
    public Cloudinary avatarCloudinary() {
        return buildClient("avatar", avatarCloudName, avatarApiKey, avatarApiSecret);
    }

    @Bean("courseImageCloudinary")
    public Cloudinary courseImageCloudinary() {
        return buildClient("course-image", courseImageCloudName, courseImageApiKey, courseImageApiSecret);
    }

    @Bean("courseVideoCloudinary")
    public Cloudinary courseVideoCloudinary() {
        return buildClient("course-video", courseVideoCloudName, courseVideoApiKey, courseVideoApiSecret);
    }

    private Cloudinary buildClient(String accountName, String cloudName, String apiKey, String apiSecret) {
        String normalizedCloudName = normalize(cloudName);
        String normalizedApiKey = normalize(apiKey);
        String normalizedApiSecret = normalize(apiSecret);

        validateCloudinaryConfig(accountName, normalizedCloudName, normalizedApiKey, normalizedApiSecret);
        log.info("Cloudinary account '{}' initialized with cloud_name='{}'", accountName, normalizedCloudName);

        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", normalizedCloudName,
                "api_key", normalizedApiKey,
                "api_secret", normalizedApiSecret,
                "secure", true
        ));
    }

    private void validateCloudinaryConfig(String accountName, String cloudName, String apiKey, String apiSecret) {
        if (cloudName.isEmpty() || apiKey.isEmpty() || apiSecret.isEmpty()) {
            throw new IllegalStateException("Missing Cloudinary config for account: " + accountName);
        }
        if (!cloudName.matches("^[a-z0-9_-]+$")) {
            throw new IllegalStateException("Invalid Cloudinary cloud_name for account " + accountName + ": " + cloudName);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }
}
