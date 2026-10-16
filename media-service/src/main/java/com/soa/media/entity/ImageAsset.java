package com.soa.media.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Bản ghi ảnh trong DB. Chỉ lưu metadata + public_id (KHÔNG lưu binary).
 * Các service khác (Product, User...) chỉ cần giữ publicId/url này.
 */
@Entity
@Table(name = "image_assets")
public class ImageAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String publicId;

    @Column(nullable = false, length = 1000)
    private String url;

    private String format;
    private Integer width;
    private Integer height;
    private Long bytes;
    private String originalFilename;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Long getBytes() { return bytes; }
    public void setBytes(Long bytes) { this.bytes = bytes; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public Instant getCreatedAt() { return createdAt; }
}
