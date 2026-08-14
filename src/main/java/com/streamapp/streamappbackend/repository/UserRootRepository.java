package com.streamapp.streamappbackend.repository;

import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.entity.UserRoot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRootRepository extends JpaRepository<UserRoot, Long> {

    List<UserRoot> findAllByUser(User user);

    Optional<UserRoot> findByUserAndPath(User user, String path);

    long countByUser(User user);
}