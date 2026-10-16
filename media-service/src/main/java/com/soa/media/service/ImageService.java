package com.soa.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.api.exceptions.NotFound;
import com.cloudinary.utils.ObjectUtils;
import com.soa.media.dto.ImageResponse;
import com.soa.media.dto.SignatureResponse;
import com.soa.media.entity.ImageAsset;
import com.soa.media.exception.ExternalServiceException;
import com.soa.media.exception.NotFoundException;
import com.soa.media.repository.ImageAssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Toàn bộ logic làm việc với Cloudinary:
 *  - upload qua backend (mô hình A)
 *  - ký chữ ký cho upload trực tiếp từ client (mô hình B) + đăng ký ảnh sau khi upload
 *  - sinh URL tối ưu (transformation) và xóa ảnh
 */
@Service
@SuppressWarnings({"rawtypes", "unchecked"})
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final String ALLOWED_FORMATS = "jpg,png,webp,gif";
    private static final Set<String> ALLOWED_CROPS =
            Set.of("fill", "fit", "scale", "thumb", "limit", "pad", "crop");
    private static final Pattern SAFE_PATH = Pattern.compile("^[a-zA-Z0-9_\\-/]{1,200}$");

    private final Cloudinary cloudinary;
    private final ImageAssetRepository repository;
    private final String defaultFolder;

    public ImageService(Cloudinary cloudinary,
                        ImageAssetRepository repository,
                        @Value("${app.media.default-folder:soa/demo}") String defaultFolder) {
        this.cloudinary = cloudinary;
        this.repository = repository;
        this.defaultFolder = defaultFolder;
    }

    // ------------------------------------------------------------------
    // Mô hình A: upload qua backend
    // ------------------------------------------------------------------
    public ImageResponse upload(MultipartFile file, String folder) {
        validate(file);
        String targetFolder = resolveFolder(folder);

        Map<?, ?> result;
        try {
            result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", targetFolder,
                    "resource_type", "image",
                    "unique_filename", true,
                    "overwrite", false));
        } catch (IOException | RuntimeException e) {
            throw new ExternalServiceException("Không thể upload ảnh lên Cloudinary: " + e.getMessage(), e);
        }

        ImageAsset saved = repository.save(toEntity(result, file.getOriginalFilename()));
        log.info("Đã upload ảnh {} ({} bytes)", saved.getPublicId(), saved.getBytes());
        return ImageResponse.from(saved);
    }

    // ------------------------------------------------------------------
    // Mô hình B: signed upload trực tiếp từ client
    // ------------------------------------------------------------------
    public SignatureResponse createUploadSignature(String folder) {
        String targetFolder = resolveFolder(folder);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        // Chỉ ký những tham số mà client BẮT BUỘC phải gửi đúng y như vậy.
        Map<String, Object> toSign = new TreeMap<>();
        toSign.put("timestamp", timestamp);
        toSign.put("folder", targetFolder);
        toSign.put("allowed_formats", ALLOWED_FORMATS);

        String signature = cloudinary.apiSignRequest(toSign, cloudinary.config.apiSecret);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("timestamp", timestamp);
        params.put("folder", targetFolder);
        params.put("allowed_formats", ALLOWED_FORMATS);
        params.put("signature", signature);

        String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudinary.config.cloudName + "/image/upload";
        return new SignatureResponse(cloudinary.config.cloudName, cloudinary.config.apiKey, uploadUrl, params);
    }

    /** Sau khi client upload trực tiếp xong: backend hỏi lại Cloudinary để lấy metadata đáng tin cậy rồi lưu DB. */
    public ImageResponse register(String publicId) {
        if (!SAFE_PATH.matcher(publicId).matches()) {
            throw new IllegalArgumentException("publicId không hợp lệ");
        }
        Optional<ImageAsset> existing = repository.findByPublicId(publicId);
        if (existing.isPresent()) {
            return ImageResponse.from(existing.get());
        }
        Map<?, ?> resource;
        try {
            resource = cloudinary.api().resource(publicId, ObjectUtils.emptyMap());
        } catch (NotFound e) {
            throw new IllegalArgumentException("Ảnh '" + publicId + "' chưa tồn tại trên Cloudinary");
        } catch (Exception e) {
            throw new ExternalServiceException("Không thể lấy thông tin ảnh từ Cloudinary: " + e.getMessage(), e);
        }
        return ImageResponse.from(repository.save(toEntity(resource, null)));
    }

    // ------------------------------------------------------------------
    // Truy vấn / xóa
    // ------------------------------------------------------------------
    public List<ImageResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(ImageResponse::from).toList();
    }

    public ImageResponse get(Long id) {
        return ImageResponse.from(find(id));
    }

    /** Xóa trên Cloudinary trước, rồi xóa DB => tránh "ảnh mồ côi" tốn dung lượng. */
    public void delete(Long id) {
        ImageAsset asset = find(id);
        try {
            cloudinary.uploader().destroy(asset.getPublicId(), ObjectUtils.asMap("invalidate", true));
        } catch (IOException | RuntimeException e) {
            throw new ExternalServiceException("Không thể xóa ảnh trên Cloudinary: " + e.getMessage(), e);
        }
        repository.delete(asset);
    }

    // ------------------------------------------------------------------
    // Image optimization: sinh URL biến đổi (không tốn băng thông backend)
    // ------------------------------------------------------------------
    public String optimizedUrl(String publicId, int width, int height, String crop) {
        if (!SAFE_PATH.matcher(publicId).matches()) {
            throw new IllegalArgumentException("publicId không hợp lệ");
        }
        String safeCrop = (crop == null || crop.isBlank()) ? "fill" : crop;
        if (!ALLOWED_CROPS.contains(safeCrop)) {
            throw new IllegalArgumentException("crop phải là một trong: " + ALLOWED_CROPS);
        }
        int w = clamp(width), h = clamp(height);
        return url(publicId, new Transformation()
                .width(w).height(h).crop(safeCrop).gravity("auto")
                .quality("auto").fetchFormat("auto"));
    }

    /** Một ảnh -> nhiều biến thể dùng cho nhiều vị trí hiển thị khác nhau. */
    public Map<String, String> variants(Long id) {
        ImageAsset a = find(id);
        String p = a.getPublicId();
        Map<String, String> v = new LinkedHashMap<>();
        v.put("original", a.getUrl());
        v.put("thumbnail", url(p, new Transformation().width(150).height(150).crop("fill")
                .gravity("auto").quality("auto").fetchFormat("auto")));
        v.put("card", url(p, new Transformation().width(400).height(300).crop("fill")
                .gravity("auto").quality("auto").fetchFormat("auto")));
        v.put("banner", url(p, new Transformation().width(1200).height(400).crop("fill")
                .gravity("auto").quality("auto").fetchFormat("auto")));
        v.put("avatar", url(p, new Transformation().width(200).height(200).crop("thumb")
                .gravity("face").radius("max").quality("auto").fetchFormat("auto")));
        v.put("grayscale", url(p, new Transformation().width(400).crop("scale")
                .effect("grayscale").quality("auto").fetchFormat("auto")));
        v.put("blur-placeholder", url(p, new Transformation().width(30).crop("scale")
                .effect("blur:1000").quality("auto").fetchFormat("auto")));
        return v;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private String url(String publicId, Transformation transformation) {
        return cloudinary.url().transformation(transformation).secure(true).generate(publicId);
    }

    private ImageAsset find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ảnh id=" + id));
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn file");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Chỉ chấp nhận JPEG, PNG, WEBP hoặc GIF");
        }
    }

    private String resolveFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return defaultFolder;
        }
        if (!SAFE_PATH.matcher(folder).matches()) {
            throw new IllegalArgumentException("Tên folder chỉ gồm chữ, số, '_', '-', '/'");
        }
        return folder;
    }

    private int clamp(int value) {
        return Math.max(16, Math.min(value, 4000));
    }

    private ImageAsset toEntity(Map<?, ?> res, String originalFilename) {
        ImageAsset a = new ImageAsset();
        a.setPublicId((String) res.get("public_id"));
        a.setUrl((String) res.get("secure_url"));
        a.setFormat((String) res.get("format"));
        a.setWidth(res.get("width") instanceof Number n ? n.intValue() : null);
        a.setHeight(res.get("height") instanceof Number n ? n.intValue() : null);
        a.setBytes(res.get("bytes") instanceof Number n ? n.longValue() : null);
        String name = originalFilename != null ? originalFilename : (String) res.get("original_filename");
        a.setOriginalFilename(name);
        return a;
    }
}
