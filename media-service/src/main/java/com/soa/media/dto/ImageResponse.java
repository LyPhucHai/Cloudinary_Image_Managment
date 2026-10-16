package com.soa.media.dto;

import com.soa.media.entity.ImageAsset;

import java.time.Instant;

public record ImageResponse(
        Long id,
        String publicId,
        String url,
        String format,
        Integer width,
        Integer height,
        Long bytes,
        String originalFilename,
        Instant createdAt) {

    public static ImageResponse from(ImageAsset a) {
        return new ImageResponse(a.getId(), a.getPublicId(), a.getUrl(), a.getFormat(),
                a.getWidth(), a.getHeight(), a.getBytes(), a.getOriginalFilename(), a.getCreatedAt());
    }
}
