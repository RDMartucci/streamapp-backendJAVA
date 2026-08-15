package com.streamapp.streamappbackend.repository;

import com.streamapp.streamappbackend.entity.Playlist;
import com.streamapp.streamappbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    List<Playlist> findAllByOwner(User owner);

    Optional<Playlist> findByIdAndOwner(Long id, User owner);

    @Modifying
    @Query(value = "DELETE FROM playlist_items WHERE media_item_id IN :ids", nativeQuery = true)
    void deleteItemsByMediaItemIds(@Param("ids") Collection<Long> ids);
}