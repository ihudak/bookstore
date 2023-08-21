package com.dynatrace.ratings.repository;

import com.dynatrace.ratings.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByEmail(String email);
    List<Rating> findByIsbn(String isbn);
    List<Rating> findByEmailAndIsbn(String email, String isbn);
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE ratings", nativeQuery = true)
    void truncateTable();
}
