package com.dynatrace.storage.repository;

import com.dynatrace.storage.model.Storage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface StorageRepository extends JpaRepository<Storage, Long> {
    Storage findByIsbn(String isbn);
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE storage", nativeQuery = true)
    void truncateTable();
}
