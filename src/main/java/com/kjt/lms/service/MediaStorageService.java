package com.kjt.lms.service;

import com.kjt.lms.model.response.media.MediaUploadResponse;
import org.springframework.web.multipart.MultipartFile;


public interface MediaStorageService {

    MediaUploadResponse uploadCourseImage(MultipartFile file);

    MediaUploadResponse uploadCourseVideo(MultipartFile file);

    void deleteMedia(String publicId, String resourceType);
}

