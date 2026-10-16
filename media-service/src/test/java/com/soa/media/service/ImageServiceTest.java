package com.soa.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.soa.media.dto.SignatureResponse;
import com.soa.media.repository.ImageAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

/** Test không cần mạng/key thật: chỉ kiểm tra logic sinh URL, ký chữ ký và validate. */
class ImageServiceTest {

    private ImageService service;

    @BeforeEach
    void setUp() {
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "demo", "api_key", "key", "api_secret", "secret", "secure", true));
        service = new ImageService(cloudinary, Mockito.mock(ImageAssetRepository.class), "soa/demo");
    }

    @Test
    void optimizedUrlContainsTransformations() {
        String url = service.optimizedUrl("soa/demo/cat", 400, 300, "fill");
        assertTrue(url.startsWith("https://res.cloudinary.com/demo/image/upload/"));
        assertTrue(url.contains("w_400"));
        assertTrue(url.contains("h_300"));
        assertTrue(url.contains("c_fill"));
        assertTrue(url.contains("f_auto"));
        assertTrue(url.contains("q_auto"));
        assertTrue(url.endsWith("soa/demo/cat"));
    }

    @Test
    void optimizedUrlRejectsBadCropAndBadPublicId() {
        assertThrows(IllegalArgumentException.class, () -> service.optimizedUrl("a", 100, 100, "hack"));
        assertThrows(IllegalArgumentException.class, () -> service.optimizedUrl("../etc/passwd?x=1", 100, 100, "fill"));
    }

    @Test
    void signatureHasRequiredParamsAndNeverLeaksSecret() {
        SignatureResponse s = service.createUploadSignature(null);
        assertEquals("demo", s.cloudName());
        assertEquals("soa/demo", s.params().get("folder"));
        assertNotNull(s.params().get("signature"));
        assertFalse(s.params().values().contains("secret"));
    }

    @Test
    void uploadRejectsNonImage() {
        MockMultipartFile pdf = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1});
        assertThrows(IllegalArgumentException.class, () -> service.upload(pdf, null));
    }

    @Test
    void uploadRejectsUnsafeFolder() {
        MockMultipartFile png = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        assertThrows(IllegalArgumentException.class, () -> service.upload(png, "../../root"));
    }
}
