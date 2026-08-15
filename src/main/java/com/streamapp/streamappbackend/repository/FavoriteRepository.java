package com.streamapp.streamappbackend.repository;

import com.streamapp.streamappbackend.entity.Favorite;
import com.streamapp.streamappbackend.entity.MediaItem;
import com.streamapp.streamappbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findAllByUser(User user);

    Optional<Favorite> findByUserAndMediaItem(User user, MediaItem mediaItem);

    void deleteByUserAndMediaItem(User user, MediaItem mediaItem);

    void deleteByMediaItemIn(List<MediaItem> mediaItems);
}