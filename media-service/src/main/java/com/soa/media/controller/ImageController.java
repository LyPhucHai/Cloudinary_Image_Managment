package com.soa.media.controller;

import com.soa.media.dto.ImageResponse;
import com.soa.media.dto.RegisterRequest;
import com.soa.media.dto.SignatureResponse;
import com.soa.media.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Images", description = "Upload, tối ưu và phân phối ảnh qua Cloudinary")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @Operation(summary = "Mô hình A: upload ảnh qua backend")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageResponse> upload(@RequestParam("file") MultipartFile file,
                                                @RequestParam(value = "folder", required = false) String folder) {
        return ResponseEntity.status(HttpStatus.CREATED).body(imageService.upload(file, folder));
    }

    @Operation(summary = "Danh sách ảnh đã lưu")
    @GetMapping
    public List<ImageResponse> list() {
        return imageService.list();
    }

    @Operation(summary = "Chi tiết một ảnh")
    @GetMapping("/{id}")
    public ImageResponse get(@PathVariable Long id) {
        return imageService.get(id);
    }

    @Operation(summary = "Các biến thể đã tối ưu của ảnh (thumbnail, card, banner, avatar...)")
    @GetMapping("/{id}/variants")
    public Map<String, String> variants(@PathVariable Long id) {
        return imageService.variants(id);
    }

    @Operation(summary = "Sinh URL tối ưu theo kích thước tuỳ ý (f_auto, q_auto)")
    @GetMapping("/optimized")
    public Map<String, String> optimized(@RequestParam String publicId,
                                         @RequestParam(defaultValue = "400") int w,
                                         @RequestParam(defaultValue = "400") int h,
                                         @RequestParam(defaultValue = "fill") String crop) {
        return Map.of("url", imageService.optimizedUrl(publicId, w, h, crop));
    }

    @Operation(summary = "Mô hình B (bước 1): xin chữ ký để upload trực tiếp lên Cloudinary")
    @GetMapping("/signature")
    public SignatureResponse signature(@RequestParam(value = "folder", required = false) String folder) {
        return imageService.createUploadSignature(folder);
    }

    @Operation(summary = "Mô hình B (bước 2): đăng ký ảnh đã upload trực tiếp vào DB")
    @PostMapping("/register")
    public ResponseEntity<ImageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(imageService.register(request.publicId()));
    }

    @Operation(summary = "Xóa ảnh (Cloudinary + DB)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        imageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
