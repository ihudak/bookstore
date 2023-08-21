package com.dynatrace.clients.repository;

import com.dynatrace.clients.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Client findByEmail(String email);
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE clients", nativeQuery = true)
    void truncateTable();
}
