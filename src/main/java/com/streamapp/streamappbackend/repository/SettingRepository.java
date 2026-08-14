package com.streamapp.streamappbackend.repository;

import com.streamapp.streamappbackend.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<Setting, String> {
}