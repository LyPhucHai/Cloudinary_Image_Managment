package com.soa.media.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tạo bean Cloudinary dùng chung cho toàn service.
 * api_secret chỉ tồn tại ở backend (lấy từ biến môi trường), KHÔNG bao giờ gửi xuống client.
 */
@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }
}
