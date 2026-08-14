package com.streamapp.streamappbackend.repository;

import com.streamapp.streamappbackend.entity.Playlist;
import com.streamapp.streamappbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    List<Playlist> findAllByOwner(User owner);

    Optional<Playlist> findByIdAndOwner(Long id, User owner);
}