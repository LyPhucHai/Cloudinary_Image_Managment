package com.soa.media.exception;

/** Lỗi khi gọi Cloudinary (mạng, key sai, quota...). */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
