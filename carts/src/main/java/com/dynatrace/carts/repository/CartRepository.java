package com.dynatrace.carts.repository;

import com.dynatrace.carts.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByEmail(String email);
    List<Cart> findByIsbn(String isbn);

    Cart findByEmailAndIsbn(String email, String isbn);
    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE cart", nativeQuery = true)
    void truncateTable();
}
