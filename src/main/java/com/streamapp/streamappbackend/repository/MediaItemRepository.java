package com.streamapp.streamappbackend.repository;

import com.streamapp.streamappbackend.entity.MediaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MediaItemRepository extends JpaRepository<MediaItem, Long> {

    Optional<MediaItem> findByPath(String path);
}