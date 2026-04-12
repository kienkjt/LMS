package com.kjt.lms.model.response.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadResponse {

    /**
     * URL aman untuk mengakses media (https)
     */
    private String secureUrl;

    /**
     * Public ID dari media di Cloudinary (untuk delete/update)
     */
    private String publicId;

    /**
     * Resource type: image, video, raw
     */
    private String resourceType;

    /**
     * Ukuran file dalam bytes
     */
    private Long size;

    /**
     * Durasi video dalam detik (jika video)
     */
    private Double duration;
}

