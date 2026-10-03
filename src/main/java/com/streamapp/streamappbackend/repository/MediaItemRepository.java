package com.streamapp.streamappbackend.repository;

import com.streamapp.streamappbackend.entity.MediaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MediaItemRepository extends JpaRepository<MediaItem, Long>, JpaSpecificationExecutor<MediaItem> {

    Optional<MediaItem> findByPath(String path);

    List<MediaItem> findAllByTmdbIdAndMediaTypeDetail(Long tmdbId, String mediaTypeDetail);

    @Query("SELECT mi FROM MediaItem mi WHERE lower(function('replace', mi.path, '\\\\', '/')) = lower(:path)")
    Optional<MediaItem> findByNormalizedPath(@Param("path") String path);

    @Query("SELECT mi FROM MediaItem mi WHERE substring(mi.path, 1, length(:prefix)) = :prefix")
    List<MediaItem> findByPathPrefix(@Param("prefix") String prefix);

    @Query("SELECT mi FROM MediaItem mi WHERE lower(function('replace', mi.path, '\\\\', '/')) LIKE lower(concat(:prefix, '%'))")
    List<MediaItem> findByNormalizedPathPrefix(@Param("prefix") String prefix);
}