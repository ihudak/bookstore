package com.dynatrace.ingest.repository;

import com.dynatrace.ingest.model.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SettingRepository extends JpaRepository<Setting, String> {
    Setting findOneByActive(boolean isActive);

    @Query(value = "UPDATE settings SET active = false")
    @Modifying
    int deactivateAllSettings();

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE settings", nativeQuery = true)
    void truncateTable();
}
