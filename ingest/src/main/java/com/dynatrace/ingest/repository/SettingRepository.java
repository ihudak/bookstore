package com.dynatrace.ingest.repository;

import com.dynatrace.ingest.model.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingRepository extends JpaRepository<Setting, String> {
    Setting findOneByActive(boolean isActive);
}
