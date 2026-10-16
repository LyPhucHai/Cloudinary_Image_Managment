package com.soa.media.repository;

import com.soa.media.entity.ImageAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageAssetRepository extends JpaRepository<ImageAsset, Long> {
    Optional<ImageAsset> findByPublicId(String publicId);
    List<ImageAsset> findAllByOrderByCreatedAtDesc();
}
