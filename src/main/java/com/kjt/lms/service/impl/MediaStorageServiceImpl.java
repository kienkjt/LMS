package com.kjt.lms.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.model.response.media.MediaUploadResponse;
import com.kjt.lms.service.MediaStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MediaStorageServiceImpl implements MediaStorageService {

    private final Cloudinary courseImageCloudinary;
    private final Cloudinary courseVideoCloudinary;
    private final MessageProvider messageProvider;

    public MediaStorageServiceImpl(
            @Qualifier("courseImageCloudinary") Cloudinary courseImageCloudinary,
            @Qualifier("courseVideoCloudinary") Cloudinary courseVideoCloudinary,
            MessageProvider messageProvider) {
        this.courseImageCloudinary = courseImageCloudinary;
        this.courseVideoCloudinary = courseVideoCloudinary;
        this.messageProvider = messageProvider;
    }

    @Value("${app.media.course-image.max-size-bytes:52428800}")
    private long maxCourseImageSizeBytes;

    @Value("${app.media.course-image.allowed-content-types:image/jpeg,image/png,image/webp}")
    private String allowedCourseImageContentTypes;

    @Value("${app.media.course-video.max-size-bytes:5368709120}")
    private long maxCourseVideoSizeBytes;

    // Cloudinary recommends chunked upload for larger video payloads.
    private static final long LARGE_VIDEO_UPLOAD_THRESHOLD_BYTES = 100L * 1024 * 1024;
    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of("mp4", "mpeg", "mpg", "mov", "avi", "flv", "webm", "mkv");

    @Value("${app.media.course-video.allowed-content-types:video/mp4,video/mpeg,video/quicktime,video/x-msvideo,video/x-flv,video/webm}")
    private String allowedCourseVideoContentTypes;

    @Override
    public MediaUploadResponse uploadCourseImage(MultipartFile file) {
        validateCourseImageFile(file);

        try {
            Map<?, ?> uploadResult = courseImageCloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "lms/courses/images",
                            "resource_type", "image",
                            "quality", "auto:good"
                    )
            );

            return buildMediaUploadResponse(uploadResult, "image");

        } catch (IOException ex) {
            log.error("Course image upload failed", ex);
            throw new BusinessException(messageProvider.getMessage("media.course.image.upload.failed"));
        }
    }

    @Override
    public MediaUploadResponse uploadCourseVideo(MultipartFile file) {
        validateCourseVideoFile(file);

        File tempFile = null;
        try {
            tempFile = File.createTempFile("lms-course-video-", ".upload");
            file.transferTo(tempFile);

            Map<String, Object> options = ObjectUtils.asMap(
                    "folder", "lms/courses/videos",
                    "resource_type", "video",
                    "quality", "auto",
                    "eager", java.util.List.of(
                            new Transformation()
                                    .width(1280)
                                    .height(720)
                                    .crop("scale")
                                    .quality("auto")
                    ),
                    "eager_async", true
            );

            Map<?, ?> uploadResult;
            if (file.getSize() > LARGE_VIDEO_UPLOAD_THRESHOLD_BYTES) {
                uploadResult = courseVideoCloudinary.uploader().uploadLarge(tempFile, options);
            } else {
                uploadResult = courseVideoCloudinary.uploader().upload(tempFile, options);
            }

            return buildMediaUploadResponse(uploadResult, "video");

        } catch (IOException ex) {
            log.error("Course video upload failed", ex);
            throw new BusinessException(messageProvider.getMessage("media.course.video.upload.failed"));
        } finally {
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                log.warn("Cannot delete temp video file: {}", tempFile.getAbsolutePath());
            }
        }
    }

    @Override
    public void deleteMedia(String publicId, String resourceType) {
        if (publicId == null || publicId.isEmpty()) {
            throw new BusinessException("Public ID tidak boleh kosong");
        }

        try {
            Cloudinary cloudinary = "video".equals(resourceType)
                    ? courseVideoCloudinary
                    : courseImageCloudinary;

            Map<?, ?> deleteResult = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", resourceType
            ));

            String result = (String) deleteResult.get("result");
            if ("ok".equals(result)) {
                log.info("Media deleted successfully: {} (type: {})", publicId, resourceType);
            } else {
                log.warn("Media deletion may have failed: {} (type: {})", publicId, resourceType);
            }

        } catch (IOException ex) {
            log.error("Media deletion failed: {} - {}", publicId, ex.getMessage());
            throw new BusinessException(messageProvider.getMessage("media.delete.failed"));
        }
    }

    private void validateCourseImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(messageProvider.getMessage("validation.course.image.required"));
        }

        if (file.getSize() > maxCourseImageSizeBytes) {
            throw new BusinessException(
                    messageProvider.getMessage("validation.course.image.maxSize", maxCourseImageSizeBytes));
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException(messageProvider.getMessage("validation.course.image.invalidType"));
        }

        String normalizedContentType = contentType.toLowerCase(Locale.ROOT).trim();
        if (!getAllowedCourseImageContentTypes().contains(normalizedContentType)) {
            throw new BusinessException(messageProvider.getMessage("validation.course.image.invalidType"));
        }
    }

    private void validateCourseVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(messageProvider.getMessage("validation.course.video.required"));
        }

        if (file.getSize() > maxCourseVideoSizeBytes) {
            throw new BusinessException(
                    messageProvider.getMessage("validation.course.video.maxSize", maxCourseVideoSizeBytes));
        }

        String contentType = file.getContentType();
        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).trim();

        boolean allowedByContentType = !normalizedContentType.isEmpty()
                && getAllowedCourseVideoContentTypes().contains(normalizedContentType);
        boolean allowedByExtension = isAllowedVideoExtension(file.getOriginalFilename());

        if (!allowedByContentType && !allowedByExtension) {
            log.warn("Rejected course video upload. contentType={}, filename={}", contentType, file.getOriginalFilename());
            throw new BusinessException(messageProvider.getMessage("validation.course.video.invalidType"));
        }
    }

    private boolean isAllowedVideoExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return false;
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT).trim();
        return ALLOWED_VIDEO_EXTENSIONS.contains(extension);
    }

    private Set<String> getAllowedCourseImageContentTypes() {
        return Arrays.stream(allowedCourseImageContentTypes.split(","))
                .map(value -> value.toLowerCase(Locale.ROOT).trim())
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }

    private Set<String> getAllowedCourseVideoContentTypes() {
        return Arrays.stream(allowedCourseVideoContentTypes.split(","))
                .map(value -> value.toLowerCase(Locale.ROOT).trim())
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }

    private MediaUploadResponse buildMediaUploadResponse(Map<?, ?> uploadResult, String resourceType) {
        Object secureUrl = uploadResult.get("secure_url");
        Object publicId = uploadResult.get("public_id");
        Object bytes = uploadResult.get("bytes");
        Object duration = uploadResult.get("duration");

        if (secureUrl == null || publicId == null) {
            throw new BusinessException("Upload response không chứa URL hoặc public ID");
        }

        return MediaUploadResponse.builder()
                .secureUrl(secureUrl.toString())
                .publicId(publicId.toString())
                .resourceType(resourceType)
                .size(bytes != null ? Long.valueOf(bytes.toString()) : null)
                .duration(duration != null ? Double.valueOf(duration.toString()) : null)
                .build();
    }
}
