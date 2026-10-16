package com.soa.media.dto;

import java.util.Map;

/** Dữ liệu client cần để upload trực tiếp lên Cloudinary (mô hình B). */
public record SignatureResponse(
        String cloudName,
        String apiKey,
        String uploadUrl,
        Map<String, String> params) {
}
