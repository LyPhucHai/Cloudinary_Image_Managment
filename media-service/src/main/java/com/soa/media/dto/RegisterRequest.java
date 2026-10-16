package com.soa.media.dto;

import jakarta.validation.constraints.NotBlank;

/** Client báo cho backend biết ảnh đã upload trực tiếp xong (mô hình B). */
public record RegisterRequest(@NotBlank String publicId) {
}
